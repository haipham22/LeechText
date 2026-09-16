#!/usr/bin/env python3
"""Desktop dogfood hub: Xvfb + xdotool — chup anh + click/go phim app desktop Linux.

Y hệt mobile_gateway: ảnh screenshot trả ImageContent (base64) cho LLM xem
ngay + lưu .dogfood/, log mỗi phiên lưu .dogfood/<label>-<ts>.log.

2 chế độ gọi:
  1. CLI (chính — giữ state Xvfb/app qua file):
     python3 tools/desktop_gateway.py start '{"app": "./gradlew :desktop-app:run"}'
     python3 tools/desktop_gateway.py call desktop_screenshot '{"label": "home"}'
     python3 tools/desktop_gateway.py call desktop_scenario '{"steps": [...]}'
  2. MCP stdio (nếu muốn nối như MCP server):
     python3 tools/desktop_gateway.py --stdio

State (pid Xvfb, pid app, window id) lưu /tmp/desktop-gw-state.json — mỗi lần
gọi là 1 process mới nhưng nối lại được Xvfb/app đang chạy.

Can trong PATH: Xvfb, xdotool, ffmpeg.
"""

import base64
import json
import os
import re
import shutil
import subprocess
import sys
import time
from typing import Any

DISPLAY = os.environ.get("DESKTOP_GW_DISPLAY", ":99")
SIZE = os.environ.get("DESKTOP_GW_SIZE", "1440x900x24")
APP_LOG = "/tmp/desktop-gw-app.log"
STATE_FILE = "/tmp/desktop-gw-state.json"
PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOGFOOD_DIR = os.path.join(PROJECT_ROOT, ".dogfood")
SESSION_LOG = os.path.join(DOGFOOD_DIR, "desktop-session.log")
STATE: dict[str, Any] = {"xvfb_pid": None, "app_pid": None}


def _session_log(line: str) -> None:
    """Append moi lenh vao .dogfood/desktop-session.log — audit trail ca phien."""
    os.makedirs(DOGFOOD_DIR, exist_ok=True)
    with open(SESSION_LOG, "a") as f:
        f.write(f"{time.strftime('%H:%M:%S')} {line}\n")


def _have(bin: str) -> bool:
    return shutil.which(bin) is not None


def _disp_env() -> dict:
    env = dict(os.environ)
    env["DISPLAY"] = DISPLAY
    return env


def _run(cmd: list[str] | str, shell: bool = False, timeout: float = 60) -> subprocess.CompletedProcess:
    return subprocess.run(cmd, shell=shell, capture_output=True, text=True, timeout=timeout, env=_disp_env())


def _load_state() -> None:
    try:
        with open(STATE_FILE) as f:
            STATE.update(json.load(f))
    except (FileNotFoundError, json.JSONDecodeError):
        pass


def _save_state() -> None:
    with open(STATE_FILE, "w") as f:
        json.dump(STATE, f)


def _pid_alive(pid: int | None) -> bool:
    if not pid:
        return False
    try:
        os.kill(pid, 0)
        return True
    except (OSError, ValueError, TypeError):
        return False


def _xvfb_alive() -> bool:
    return _pid_alive(STATE.get("xvfb_pid"))


def _find_window(pattern: str = "LeechText") -> dict | None:
    """Tim cua so app trong Xvfb → {id, x, y, w, h} (offset goc man hinh)."""
    r = _run(["xwininfo", "-root", "-children"])
    if r.returncode != 0:
        return None
    m = re.search(
        rf'^\s+(0x[0-9a-f]+) "{re.escape(pattern)}".*?(\d+)x(\d+)\+(\d+)\+(\d+)',
        r.stdout, re.MULTILINE,
    )
    if not m:
        return None
    return {"id": m.group(1), "w": int(m.group(2)), "h": int(m.group(3)),
            "x": int(m.group(4)), "y": int(m.group(5))}


# ─────────────────────────── TOOLS ───────────────────────────
# Mỗi tool trả dict {"text": str, "image": path|None} — CLI in text + path,
# MCP wrapper bọc thành TextContent + ImageContent (ảnh base64 như mobile-mcp).

def t_start(app: str, title: str = "LeechText", size: str = SIZE) -> dict:
    missing = [b for b in ("Xvfb", "xdotool", "ffmpeg") if not _have(b)]
    if missing:
        return {"text": f"error: thieu binary {missing} — apt-get install -y xvfb xdotool ffmpeg"}
    if not _xvfb_alive():
        xvfb = subprocess.Popen(
            ["Xvfb", DISPLAY, "-screen", "0", size],
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
        )
        STATE["xvfb_pid"] = xvfb.pid
        time.sleep(1.5)
    if _pid_alive(STATE.get("app_pid")):
        os.kill(STATE["app_pid"], 15)
        time.sleep(1)
    app_proc = subprocess.Popen(
        app, shell=True, stdout=open(APP_LOG, "w"), stderr=subprocess.STDOUT, env=_disp_env(),
    )
    STATE["app_pid"] = app_proc.pid
    _save_state()
    deadline = time.time() + 180
    while time.time() < deadline:
        if not _pid_alive(app_proc.pid):
            return {"text": f"error: app chet som — xem {APP_LOG}"}
        win = _find_window(title)
        if win and win["w"] > 10:
            _run(["xdotool", "windowactivate", "--sync", win["id"]])
            time.sleep(2.5)  # cho UI render + fetch data
            return {"text": f"started: window {win['id']} {win['w']}x{win['h']}+{win['x']}+{win['y']} display {DISPLAY}"}
        time.sleep(1)
    return {"text": f"error: khong thay cua so '{title}' sau 180s — xem {APP_LOG}"}


def _capture_window_png() -> bytes | None:
    win = _find_window()
    if not win:
        return None
    r = _run(
        f"xwd -root -silent -out /tmp/desktop-gw.xwd && ffmpeg -y -loglevel error "
        f"-f xwd_pipe -i /tmp/desktop-gw.xwd -vf "
        f"crop={win['w']}:{win['h']}:{win['x']}:{win['y']} /tmp/desktop-gw.png",
        shell=True,
    )
    if r.returncode != 0:
        return None
    with open("/tmp/desktop-gw.png", "rb") as f:
        return f.read()


def _save_image(png: bytes, label: str, step: int) -> str:
    os.makedirs(DOGFOOD_DIR, exist_ok=True)
    path = os.path.join(DOGFOOD_DIR, f"{label}-step{step:02d}-{int(time.time() * 1000)}.png")
    with open(path, "wb") as f:
        f.write(png)
    return path


def t_screenshot(label: str = "desktop", step: int = 0) -> dict:
    """Chup cua so app: tra image base64 (LLM xem ngay) + luu .dogfood/."""
    png = _capture_window_png()
    if png is None:
        return {"text": "error: khong chup duoc — desktop_start chua? cua so mat?"}
    path = _save_image(png, label, step)
    return {"text": f"saved {path}", "image": path}


def t_click(x: int, y: int, button: int = 1) -> dict:
    win = _find_window()
    if not win:
        return {"text": "error: khong tim thay cua so app"}
    r = _run(["xdotool", "mousemove", "--window", win["id"], str(x), str(y), "click", str(button)])
    return {"text": "clicked" if r.returncode == 0 else f"error: {r.stderr[:200]}"}


def t_type(text: str, clear: bool = False) -> dict:
    if clear:
        _run(["xdotool", "key", "--clearmodifiers", "ctrl+a"])
        _run(["xdotool", "key", "Delete"])
    r = _run(["xdotool", "type", "--delay", "30", "--", text])
    return {"text": "typed" if r.returncode == 0 else f"error: {r.stderr[:200]}"}


def t_key(key: str) -> dict:
    r = _run(["xdotool", "key", "--", key])
    return {"text": "pressed" if r.returncode == 0 else f"error: {r.stderr[:200]}"}


def t_scroll(y: int = 5, x: int = 0, at: list | None = None) -> dict:
    # at=[x,y] — cuộn TẠI VỊ TRÍ cửa sổ (mousemove trước, không thì wheel rơi vào
    # control cuối nhận focus — list không cuộn)
    if at:
        win = _find_window()
        if win:
            _run(["xdotool", "mousemove", "--window", win["id"], str(at[0]), str(at[1])])
    for _ in range(abs(y)):
        _run(["xdotool", "click", "5" if y > 0 else "4"])
    for _ in range(abs(x)):
        _run(["xdotool", "click", "7" if x > 0 else "6"])
    return {"text": f"scrolled y={y} x={x} at={at}"}


def t_windows() -> dict:
    r = _run(["xwininfo", "-root", "-children"])
    lines = [l.strip() for l in r.stdout.splitlines() if re.search(r'0x[0-9a-f]+ "', l)][:20]
    return {"text": "\n".join(lines) or "error: khong doc duoc tree cua so"}


def t_app_log(lines: int = 60, grep: str | None = None) -> dict:
    """Doc log app (stdout+stderr). Args: {"grep": "Exception", "lines": 200}."""
    try:
        with open(APP_LOG) as f:
            text = f.read()
    except FileNotFoundError:
        return {"text": "(chua co log — desktop_start chua chay?)"}
    if grep:
        try:
            text = "\n".join(l for l in text.splitlines() if re.search(grep, l))
        except re.error:
            text = "\n".join(l for l in text.splitlines() if grep in l)
    return {"text": "\n".join(text.splitlines()[-lines:]) or "(trong)"}


def t_stop() -> dict:
    for key in ("app_pid", "xvfb_pid"):
        pid = STATE.get(key)
        if _pid_alive(pid):
            try:
                os.kill(pid, 15)
            except OSError:
                pass
    STATE.update(app_pid=None, xvfb_pid=None)
    _save_state()
    time.sleep(0.5)
    return {"text": "stopped"}


def t_scenario(steps: list, label: str = "desktop") -> dict:
    """Chay chuoi buoc 1 lan goi — y het mobile_scenario: log .dogfood/, anh tu dong.

    Step: {"tool": "desktop_click", "x": 100, "y": 200} hoac {"sleep": 1000} (ms)
    hoac {"screenshot": true} (chup anh truoc khi thuc hien step ke tiep).
    Cuoi phiên: dump app log (loc Exception/FATAL) vao .dogfood/<label>-<ts>-app.log.
    """
    os.makedirs(DOGFOOD_DIR, exist_ok=True)
    stamp = int(time.time() * 1000)
    log_lines: list[str] = []
    images: list[str] = []
    fns = {"desktop_click": t_click, "desktop_type": t_type, "desktop_key": t_key,
           "desktop_scroll": t_scroll, "desktop_screenshot": t_screenshot,
           "desktop_app_log": t_app_log, "desktop_windows": t_windows}
    for i, step in enumerate(steps):
        if "sleep" in step:
            time.sleep(float(step["sleep"]) / 1000.0)
            log_lines.append(f"step {i}: slept {step['sleep']}ms")
            continue
        if step.get("screenshot"):
            shot = t_screenshot(label, i)
            log_lines.append(f"step {i} screenshot: {shot['text']}")
            if shot.get("image"):
                images.append(shot["image"])
            continue
        tool = step.get("tool")
        if not tool:
            log_lines.append(f"step {i}: MISSING tool — bo qua")
            continue
        fn = fns.get(tool)
        if fn is None:
            log_lines.append(f"step {i}: unknown tool {tool}")
            continue
        args = {k: v for k, v in step.items() if k != "tool"}
        if tool == "desktop_screenshot":
            args.setdefault("label", label)
            args.setdefault("step", i)
        try:
            r = fn(**args)
            log_lines.append(f"step {i} {tool}: {str(r.get('text'))[:200]}")
            if r.get("image"):
                images.append(r["image"])
        except Exception as e:
            log_lines.append(f"step {i} {tool}: EXCEPTION {e}")
    # App log — dump + loc loi nhu mobile scenario dump logcat
    err = t_app_log(lines=80, grep=r"Exception|FATAL|SEVERE|\[Engine\].*error")["text"]
    app_log_path = os.path.join(DOGFOOD_DIR, f"{label}-{stamp}-app.log")
    try:
        with open(APP_LOG) as f:
            shutil.copyfileobj(f, open(app_log_path, "w"))
    except FileNotFoundError:
        pass
    log_path = os.path.join(DOGFOOD_DIR, f"{label}-{stamp}.log")
    with open(log_path, "w") as f:
        f.write("\n".join(log_lines) + f"\napp errors:\n{err}\n")
    text = f"scenario done: {len(steps)} steps, log: {log_path}\napp errors:\n{err[:1200]}"
    return {"text": text, "images": images}


TOOLS = {
    "desktop_start": t_start, "desktop_screenshot": t_screenshot,
    "desktop_click": t_click, "desktop_type": t_type, "desktop_key": t_key,
    "desktop_scroll": t_scroll, "desktop_windows": t_windows,
    "desktop_app_log": t_app_log, "desktop_stop": t_stop,
    "desktop_scenario": t_scenario,
}


def _cli_call() -> int:
    """CLI: call <tool> '<json>' — load state, chay tool, log + in ket qua."""
    _load_state()
    tool = sys.argv[2] if len(sys.argv) > 2 else ""
    args = json.loads(sys.argv[3]) if len(sys.argv) > 3 else {}
    if tool not in TOOLS:
        print(f"error: unknown tool '{tool}'. Tools: {', '.join(TOOLS)}")
        return 1
    r = TOOLS[tool](**args)
    _session_log(f"{tool} {json.dumps(args, ensure_ascii=False)} -> {str(r.get('text'))[:150]}")
    print(r["text"])
    return 0


def _cli_direct() -> int:
    """CLI nhanh: python3 tools/desktop_gateway.py desktop_start '<json>'."""
    _load_state()
    cmd = sys.argv[1]
    args = json.loads(sys.argv[2]) if len(sys.argv) > 2 else {}
    r = TOOLS[cmd](**args)
    _session_log(f"{cmd} {json.dumps(args, ensure_ascii=False)} -> {str(r.get('text'))[:150]}")
    print(r["text"])
    return 0


def main() -> int:
    if "--check" in sys.argv:
        for b in ("Xvfb", "xdotool", "ffmpeg"):
            print(f"{'OK' if _have(b) else 'MISSING'}: {b}")
        return 0
    if "--stdio" in sys.argv:
        # MCP stdio — anh tra ImageContent base64 nhu mobile-mcp
        from mcp.server.fastmcp import FastMCP
        from mcp.types import ImageContent

        mcp_app = FastMCP("desktop-gateway")

        def wrap(name, fn):
            def call(**kwargs):
                r = fn(**kwargs)
                content = [types_text(r["text"])]
                for img in r.get("images") or ([r["image"]] if r.get("image") else []):
                    with open(img, "rb") as f:
                        content.append(ImageContent(
                            type="image", data=base64.b64encode(f.read()).decode(),
                            mimeType="image/png",
                        ))
                return content
            import inspect
            sig = inspect.signature(fn)
            params = [p for p in sig.parameters.values() if p.name not in ("step",)]
            call.__signature__ = sig.replace(parameters=params)
            mcp_app.tool()(call)

        def types_text(s: str):
            from mcp.types import TextContent
            return TextContent(type="text", text=s)

        for name, fn in TOOLS.items():
            if name != "desktop_scenario":
                wrap(name, fn)
        wrap("desktop_scenario", t_scenario)
        mcp_app.run()
        return 0
    if len(sys.argv) > 1:
        if sys.argv[1] == "call":
            return _cli_call()
        if sys.argv[1] in TOOLS:
            return _cli_direct()
    print(__doc__)
    return 1


if __name__ == "__main__":
    sys.exit(main())
