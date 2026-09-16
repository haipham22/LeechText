#!/usr/bin/env python3
"""MCP gateway: agent -> mobile-mcp.

Spawn mobile-mcp (stdio) lam upstream, passthrough toan bo tool qua
streamable-http de N agent ket noi cung luc:

    pip install "mcp>=1.2,<2" uvicorn
    python3 tools/mobile_gateway.py          # http://127.0.0.1:8000/mcp
    python3 tools/mobile_gateway.py --check  # smoke test

Agent ket noi (Claude Code):
    claude mcp add --transport http mobile-gateway http://127.0.0.1:8000/mcp

Env:
    MOBILE_MCP_CMD  (mac dinh: npx)
    MOBILE_MCP_ARGS (mac dinh: "-y mobile-mcp@latest")
    GATEWAY_PORT    (mac dinh: 8000)

mobile-mcp can adb trong PATH - gateway tu prepend adb cua mise Android SDK.
"""

import asyncio
import os
import sys
from contextlib import asynccontextmanager
from typing import Any

import mcp.types as types
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client
from mcp.server.fastmcp import FastMCP

UPSTREAM_CMD = os.environ.get("MOBILE_MCP_CMD", "npx")
# @mobilenext/mobile-mcp = mobile-next/mobile-mcp (Android + iOS sim).
# NHẦM hay gặp: npm "mobile-mcp" là package cũ runablehq (Android-only).
UPSTREAM_ARGS = os.environ.get("MOBILE_MCP_ARGS", "-y @mobilenext/mobile-mcp@latest").split()
GATEWAY_PORT = int(os.environ.get("GATEWAY_PORT", "8000"))

# Session upstream luu global - gateway 1 tien trinh du dung.
SESSION: dict[str, Any] = {}


def upstream_env() -> dict:
    """Env day du cho upstream - SDK loc PATH qua get_default_environment nen
    adb ngoai profile PATH bi mat; tu prepend thu muc adb cua mise Android SDK."""
    env = dict(os.environ)
    adb_dir = os.path.expanduser("~/.local/share/mise/installs/android-sdk/latest/platform-tools")
    if os.path.isdir(adb_dir):
        env["PATH"] = adb_dir + os.pathsep + env.get("PATH", "")
    return env


@asynccontextmanager
async def upstream_session():
    params = StdioServerParameters(command=UPSTREAM_CMD, args=UPSTREAM_ARGS, env=upstream_env())
    async with stdio_client(params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            yield session


@asynccontextmanager
async def gateway_lifespan(server):
    async with upstream_session() as session:
        SESSION["session"] = session
        yield {"session": session}


mcp_app = FastMCP("mobile-mcp-gateway", host="127.0.0.1", port=GATEWAY_PORT, lifespan=gateway_lifespan)

# System-level tools (wrap adb) — agent khong cham adb truc tiep qua Bash nua.
SYSTEM_TOOLS: list = [
    types.Tool(
        name="android_wake",
        description="Bat man hinh + mo khoa Android (wakeup + dismiss keyguard).",
        inputSchema={"type": "object", "properties": {}},
    ),
    types.Tool(
        name="android_wifi",
        description='Bat/tat wifi Android. Args: {"enable": true|false}',
        inputSchema={
            "type": "object",
            "properties": {"enable": {"type": "boolean"}},
            "required": ["enable"],
        },
    ),
    types.Tool(
        name="android_rotate",
        description='Xoay man hinh Android: landscape=true (ngang) / false (doc). Args: {"landscape": bool}',
        inputSchema={
            "type": "object",
            "properties": {"landscape": {"type": "boolean"}},
            "required": ["landscape"],
        },
    ),
    types.Tool(
        name="android_force_stop",
        description='Force-stop 1 app Android. Args: {"package": "dev.haipham22.leechtext"}',
        inputSchema={
            "type": "object",
            "properties": {"package": {"type": "string"}},
            "required": ["package"],
        },
    ),
    types.Tool(
        name="android_logcat",
        description='Doc logcat Android (loc grep + gioi han so dong). Args: {"grep": "FATAL", "lines": 200}',
        inputSchema={
            "type": "object",
            "properties": {"grep": {"type": "string"}, "lines": {"type": "integer"}},
        },
    ),
    types.Tool(
        name="adb_shell",
        description='Chay lenh adb shell bat ky (dung cuoi cung — uu tien tool chuyen biet). Args: {"command": "input tap 100 200"}',
        inputSchema={
            "type": "object",
            "properties": {"command": {"type": "string"}},
            "required": ["command"],
        },
    ),
]

# ponytail: register passthrough tren inner lowlevel Server (private attr) de giu
# nguyen schema tung tool - add_tool(FastMCP) chi infer schema tu signature.
# Nang cap: doi sang API chinh thuc khi FastMCP expose dynamic tool schema.
_inner = mcp_app._mcp_server


@_inner.list_tools()
async def list_tools() -> list:
    # Passthrough 1:1 + tool scenario cua gateway.
    upstream = (await SESSION["session"].list_tools()).tools
    scenario = types.Tool(
        name="mobile_scenario",
        description=(
            "Chay ca kich ban dogfood trong 1 lan goi: steps la list — "
            '{"tool": "mobile_tap", "x": 100, "y": 200} hoac {"sleep": 1000} (ms). '
            "Screenshot tra image + luu vao .dogfood/ trong project de xem lai."
        ),
        inputSchema={
            "type": "object",
            "properties": {
                "steps": {"type": "array", "items": {"type": "object"}},
                "label": {"type": "string", "description": "Tien to ten file anh (default: dogfood)"},
            },
            "required": ["steps"],
        },
    )
    # them logcat params vao schema scenario
    scenario.inputSchema["properties"]["logcat_grep"] = {
        "type": "string",
        "description": 'Regex loc device log (default: "FATAL|Exception|ANR|[Engine]")',
    }
    scenario.inputSchema["properties"]["logcat_lines"] = {
        "type": "integer",
        "description": "So dong logcat dump sau khi chay (default 800)",
    }
    return upstream + [scenario] + SYSTEM_TOOLS


def _adb_device() -> str:
    """Lay device Android dau tien dang online."""
    import subprocess

    out = subprocess.run(["adb", "devices"], capture_output=True, text=True, env=upstream_env()).stdout
    for line in out.splitlines()[1:]:
        parts = line.split()
        if len(parts) == 2 and parts[1] == "device":
            return parts[0]
    return ""


def _adb_shell(command: str, device: str | None = None) -> str:
    import subprocess

    dev = device or _adb_device()
    if not dev:
        return "error: khong co thiet bi Android nao online"
    # shell=True de ho tro chuoi nhieu lenh (a; b) qua 1 lan goi adb shell
    out = subprocess.run(f'adb -s {dev} shell "{command}"', shell=True, capture_output=True, text=True, env=upstream_env())
    return out.stdout.strip() or out.stderr.strip() or "(ok, khong co output)"


def _run_system_tool(name: str, args: dict) -> list:
    if name == "android_wake":
        r = _adb_shell("input keyevent KEYCODE_WAKEUP; wm dismiss-keyguard")
        return [types.TextContent(type="text", text=r or "waked")]
    if name == "android_wifi":
        enable = "enable" if args.get("enable") else "disable"
        return [types.TextContent(type="text", text=_adb_shell(f"svc wifi {enable}"))]
    if name == "android_rotate":
        side = "0" if args.get("landscape") else "1"
        r1 = _adb_shell("settings put system accelerometer_rotation 0")
        r2 = _adb_shell(f"settings put system user_rotation {side}")
        return [types.TextContent(type="text", text=(r1 + r2).strip() or "rotated")]
    if name == "android_force_stop":
        pkg = args.get("package") or ""
        return [types.TextContent(type="text", text=_adb_shell(f"am force-stop {pkg}"))]
    if name == "android_logcat":
        grep = args.get("grep")
        lines = int(args.get("lines") or 200)
        import re
        import subprocess

        out = subprocess.run(["adb", "-s", _adb_device(), "logcat", "-d", "-t", str(lines)], capture_output=True, text=True, env=upstream_env())
        text = out.stdout
        if grep:
            try:
                text = "\n".join(l for l in text.splitlines() if re.search(grep, l))
            except re.error:
                text = "\n".join(l for l in text.splitlines() if grep in l)
        return [types.TextContent(type="text", text=text[-4000:])]
    if name == "adb_shell":
        return [types.TextContent(type="text", text=_adb_shell(args.get("command") or ""))]
    return [types.TextContent(type="text", text=f"unknown system tool: {name}")]


@_inner.call_tool()
async def call_tool(name: str, arguments: dict | None) -> list:
    # mobile_scenario = chay ca kich ban 1 lan; android_* = wrap adb;
    # con lai passthrough 1:1 ve mobile-mcp (Android + iOS giong nhau).
    args = arguments or {}
    if name == "mobile_scenario":
        return await _run_scenario(
            args.get("steps") or [],
            args.get("label") or "dogfood",
            args.get("logcat_grep"),
            int(args.get("logcat_lines") or 800),
        )
    if name in {"android_wake", "android_wifi", "android_rotate", "android_force_stop", "android_logcat", "adb_shell"}:
        return _run_system_tool(name, args)
    result = await SESSION["session"].call_tool(name, args)
    return result.content


# Anh dogfood luu vao .dogfood/ trong project root (da git-ignore).
PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def _save_image(data_b64: str, mime: str, label: str, step: int) -> str:
    import base64
    import time

    d = os.path.join(PROJECT_ROOT, ".dogfood")
    os.makedirs(d, exist_ok=True)
    ext = "jpg" if "jpeg" in (mime or "") else "png"
    path = os.path.join(d, f"{label}-step{step:02d}-{int(time.time() * 1000)}.{ext}")
    with open(path, "wb") as f:
        f.write(base64.b64decode(data_b64))
    return path


async def _run_scenario(
    steps: list,
    label: str,
    logcat_grep: str | None = None,
    logcat_lines: int = 800,
) -> list[types.Content]:
    """Chay tuan tu cac step len mobile-mcp, tra log + anh + DEVICE LOG, luu .dogfood/.

    Step: {"tool": "mobile_tap", "x": 100, "y": 200} — tool + tham so goc.
    Step dac biet: {"sleep": 1000} — nghi ms giua cac buoc.
    Screenshot: tra image nguyen ven + luu .dogfood/<label>-stepNN-<ts>.png.
    Step log: .dogfood/<label>-<ts>.log.
    Device log (Android): clear truoc khi chay, dump sau — luu
    .dogfood/<label>-<ts>-device.log; excerpt dong FATAL/Exception/ANR/[Engine]
    tra ve trong response de thay crash ngay.
    logcat_grep: regex loc device log (default: FATAL|Exception|ANR|[Engine]).
    """
    import re
    import time

    session = SESSION["session"]
    device = _adb_device()
    if device:
        _adb_shell("logcat -c", device)
    out: list[types.Content] = []
    log_lines: list[str] = []
    dogfood_dir = os.path.join(PROJECT_ROOT, ".dogfood")
    os.makedirs(dogfood_dir, exist_ok=True)
    stamp = int(time.time() * 1000)
    for i, step in enumerate(steps):
        if "sleep" in step:
            await asyncio.sleep(float(step["sleep"]) / 1000.0)
            log_lines.append(f"step {i}: slept {step['sleep']}ms")
            continue
        tool = step.get("tool")
        if not tool:
            line = f"step {i}: MISSING 'tool' - bo qua"
            log_lines.append(line)
            out.append(types.TextContent(type="text", text=line))
            continue
        args = {k: v for k, v in step.items() if k != "tool"}
        try:
            result = await session.call_tool(tool, args)
            for c in result.content:
                if isinstance(c, types.ImageContent):
                    path = _save_image(c.data, getattr(c, "mimeType", "image/png"), label, i)
                    log_lines.append(f"step {i} {tool}: saved {path}")
                    out.append(c)  # image nguyen ven cho LLM xem ngay
                elif isinstance(c, types.TextContent):
                    line = f"step {i} {tool}: {c.text[:400]}"
                    log_lines.append(line)
                    out.append(types.TextContent(type="text", text=line))
            if getattr(result, "isError", False):
                line = f"step {i} {tool}: ERROR"
                log_lines.append(line)
                out.append(types.TextContent(type="text", text=line))
        except Exception as e:  # step loi - ghi lai, chay tiep
            line = f"step {i} {tool}: EXCEPTION {e}"
            log_lines.append(line)
            out.append(types.TextContent(type="text", text=line))
    log_path = os.path.join(dogfood_dir, f"{label}-{stamp}.log")
    with open(log_path, "w") as f:
        f.write("\n".join(log_lines) + "\n")
    summary = f"scenario done: {len(steps)} steps, log: {log_path}"

    # ── DEVICE LOG (Android): dump sau khi chay, loc + luu + excerpt loi ──
    if device:
        import subprocess

        out_log = subprocess.run(
            f'adb -s {device} logcat -d -t {logcat_lines}',
            shell=True, capture_output=True, text=True, env=upstream_env(),
        )
        device_log = out_log.stdout or ""
        grep_pattern = logcat_grep or r"FATAL|Exception|ANR|\[Engine\]"
        filtered = "\n".join(l for l in device_log.splitlines() if re.search(grep_pattern, l))
        dev_path = os.path.join(dogfood_dir, f"{label}-{stamp}-device.log")
        with open(dev_path, "w") as f:
            f.write(device_log)
        summary += f", device log: {dev_path}"
        excerpt = "\n".join(
            l for l in filtered.splitlines()
            if re.search(r"FATAL|ANR|IllegalArgumentException|IllegalStateException|NullPointerException", l)
        )[:1500]
        if excerpt:
            out.append(types.TextContent(type="text", text=f"DEVICE ERRORS:\n{excerpt}"))
        elif filtered:
            head = "\n".join(filtered.splitlines()[:15])
            out.append(types.TextContent(type="text", text=f"device log (loc, 15 dong dau):\n{head}"))
    out.append(types.TextContent(type="text", text=summary))
    return out


async def smoke_check() -> int:
    """--check: connect upstream, liet ke tool - verify pipeline khong can chay server."""
    async with upstream_session() as session:
        tools = (await session.list_tools()).tools
        print(f"OK: {len(tools)} tool tu {UPSTREAM_CMD} {' '.join(UPSTREAM_ARGS)}")
        for t in tools:
            print(f"  - {t.name}")
        return 0


def main() -> int:
    if "--check" in sys.argv:
        return asyncio.run(smoke_check())
    if "--stdio" in sys.argv:
        # stdio cho .mcp.json - cấm print ra stdout (phá protocol)
        mcp_app.run()
        return 0
    print(f"mobile-mcp gateway: http://127.0.0.1:{GATEWAY_PORT}/mcp")
    mcp_app.run(transport="streamable-http")
    return 0


if __name__ == "__main__":
    sys.exit(main())
