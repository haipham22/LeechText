#!/usr/bin/env python3
"""Goi tool qua mobile gateway — stdio (spawn gateway + mobile-mcp moi lan goi).

python3 tools/dogfood.py mobile_screenshot
python3 tools/dogfood.py mobile_tap '{"x": 100, "y": 200}'
python3 tools/dogfood.py mobile_scenario '{"label": "kiem-tra", "steps": [{"sleep": 500}, {"tool": "mobile_screenshot"}]}

Meo: goi don le mat vai gi (spawn npx). Can nhieu buoc lien tiep thi goi
mobile_scenario — 1 lenh chay ca chuoi, anh luu .dogfood/.
"""

import asyncio
import json
import subprocess
import sys


# desktop_* → desktop gateway (Xvfb + xdotool), mobile_* → mobile gateway.
# Desktop gateway: 1 process gateway HOLDS Xvfb + app state — every call would
# spawn a fresh process and lose them. State lives in /tmp/desktop-gw.json.
GATEWAYS = {
    "desktop": "tools/desktop_gateway.py",
    "mobile": "tools/mobile_gateway.py",
}


def gateway_for(tool: str) -> str:
    return GATEWAYS["desktop"] if tool.startswith("desktop_") else GATEWAYS["mobile"]


async def run(tool: str, args: dict) -> None:
    # Lazy import: desktop lane không cần mcp (module thiếu trong python3 hệ thống)
    from mcp import ClientSession, StdioServerParameters
    from mcp.client.stdio import stdio_client

    script = gateway_for(tool)
    params = StdioServerParameters(command="python3", args=[script, "--stdio"], env=dict(__import__("os").environ))
    async with stdio_client(params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            result = await session.call_tool(tool, args)
            for c in result.content:
                text = getattr(c, "text", None)
                if text:
                    print(text)
                # Ảnh (screenshot) KHÔNG in data — lưu .dogfood/ rồi in path
                # (tiết kiệm token: ảnh chỉ nạp context khi chủ động Read file)
                data = getattr(c, "data", None)
                if data:
                    import base64
                    import os
                    import time
                    d = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), ".dogfood")
                    os.makedirs(d, exist_ok=True)
                    p = os.path.join(d, f"hub-{tool}-{int(time.time() * 1000)}.png")
                    with open(p, "wb") as f:
                        f.write(base64.b64decode(data))
                    print(p)


def main() -> None:
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)
    tool = sys.argv[1]
    args = json.loads(sys.argv[2]) if len(sys.argv) > 2 else {}

    if tool.startswith("desktop_"):
        # desktop gateway có state (Xvfb + app) — gọi trực tiếp process,
        # state lưu /tmp/desktop-gw.json để lần gọi sau nối lại.
        sys.exit(subprocess.call(["python3", GATEWAYS["desktop"], "call", tool] + ([json.dumps(args)] if args else [])))

    asyncio.run(run(tool, args))


if __name__ == "__main__":
    main()
