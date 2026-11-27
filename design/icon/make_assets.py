from PIL import Image, ImageDraw, ImageFilter, ImageFont
import os, math

HERE = os.path.dirname(os.path.abspath(__file__))
CUT_RAW = Image.open(os.path.join(HERE, "tv-cutout.png")).convert("RGBA")
CUT = CUT_RAW.crop(CUT_RAW.getbbox())
FONT_B = "/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc"
FONT_R = "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"

def gradient(size, c1, c2, angle=135):
    """diagonal gradient"""
    w, h = size
    base = Image.new("RGB", (w, h))
    px = base.load()
    # projection along angle
    rad = math.radians(angle)
    dx, dy = math.cos(rad), math.sin(rad)
    denom = abs(dx) * w + abs(dy) * h
    for y in range(h):
        for x in range(w):
            t = (x * dx + y * dy) / denom
            t = min(max(t, 0), 1)
            px[x, y] = tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(3))
    return base.convert("RGBA")

def add_glow(img, center, radius, color, alpha=90):
    w, h = img.size
    layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.ellipse([center[0]-radius, center[1]-radius, center[0]+radius, center[1]+radius],
              fill=color + (alpha,))
    layer = layer.filter(ImageFilter.GaussianBlur(radius * 0.55))
    return Image.alpha_composite(img, layer)

def drop_shadow(canvas, img, pos, blur=20, alpha=110, offset=(0, 10)):
    sh = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    a = img.split()[-1].point(lambda v: int(v * alpha / 255))
    tmp = Image.new("RGBA", img.size, (0, 0, 0, 0))
    tmp.paste((0, 0, 0, 255), (0, 0, img.size[0], img.size[1]))
    tmp.putalpha(a)
    sh.paste(tmp, (pos[0] + offset[0], pos[1] + offset[1]), tmp)
    sh = sh.filter(ImageFilter.GaussianBlur(blur))
    canvas.alpha_composite(sh)
    canvas.alpha_composite(img, pos)

def fit(img, target_h):
    w, h = img.size
    return img.resize((max(1, int(w * target_h / h)), target_h), Image.LANCZOS)

ICON_BG1 = (143, 227, 239)
ICON_BG2 = (21, 145, 175)

def make_icon(size):
    S = 1024
    bg = gradient((S, S), ICON_BG1, ICON_BG2, 135)
    bg = add_glow(bg, (S*0.3, S*0.25), S*0.55, (255, 255, 255), 70)
    bg = add_glow(bg, (S*0.85, S*0.9), S*0.5, (10, 90, 120), 60)
    tv = fit(CUT, int(S * 0.84))
    drop_shadow(bg, tv, ((S - tv.size[0]) // 2, (S - tv.size[1]) // 2 + int(S*0.005)),
                blur=34, alpha=120, offset=(0, 14))
    return bg.resize((size, size), Image.LANCZOS)

def make_adaptive_fg(size=432):
    S = 1024
    tv = fit(CUT, int(S * 0.60))
    canvas = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    drop_shadow(canvas, tv, ((S-tv.size[0])//2, (S-tv.size[1])//2), blur=24, alpha=80, offset=(0, 10))
    return canvas.resize((size, size), Image.LANCZOS)

def make_adaptive_bg(size=432):
    S = 512
    bg = gradient((S, S), ICON_BG1, ICON_BG2, 135)
    bg = add_glow(bg, (S*0.3, S*0.25), S*0.55, (255, 255, 255), 70)
    return bg.resize((size, size), Image.LANCZOS)

def make_banner(w=640, h=360):
    bg = gradient((w, h), ICON_BG1, ICON_BG2, 125)
    bg = add_glow(bg, (w*0.22, h*0.2), h*0.75, (255, 255, 255), 60)
    bg = add_glow(bg, (w*0.9, h*1.0), h*0.8, (9, 80, 110), 70)
    tv = fit(CUT, int(h * 0.88))
    drop_shadow(bg, tv, (int(w*0.015), (h - tv.size[1]) // 2), blur=18, alpha=110, offset=(0, 6))
    d = ImageDraw.Draw(bg)
    title = "豆包定制 TV"
    sub = "央视 · 卫视 · 港澳台"
    k = h / 360.0
    f1 = ImageFont.truetype(FONT_B, max(10, int(46 * k)), index=2)  # SC
    f2 = ImageFont.truetype(FONT_R, max(8, int(21 * k)), index=2)
    tx = int(w * 0.465)
    bbox = d.textbbox((0, 0), title, font=f1)
    ty = int(h * 0.355)
    d.text((tx, ty), title, font=f1, fill=(255, 250, 240, 255))
    d.text((tx + int(2*k), ty + (bbox[3]-bbox[1]) + int(22*k)), sub, font=f2, fill=(255, 248, 235, 225))
    # 底部小胶囊
    pil = "直播 · 83 频道"
    f3 = ImageFont.truetype(FONT_R, max(8, int(17 * k)), index=2)
    bw = d.textbbox((0,0), pil, font=f3)
    pad = max(4, int(14 * k))
    y0 = ty + (bbox[3]-bbox[1]) + int(66*k)
    d.rounded_rectangle([tx, y0, tx + bw[2]-bw[0] + pad*2, y0 + (bw[3]-bw[1]) + pad], radius=max(4, int(14 * k)),
                        fill=(255, 249, 238, 235))
    d.text((tx + pad, y0 + pad//2), pil, font=f3, fill=(18, 74, 78, 255))
    return bg.convert("RGB")

if __name__ == "__main__":
    out = os.path.join(HERE, "out"); os.makedirs(out, exist_ok=True)
    for s in [320, 240, 160, 120, 80, 192, 144, 96, 72, 48]:
        make_icon(s).save(os.path.join(out, f"ic_launcher_{s}.png"))
    make_adaptive_fg().save(os.path.join(out, "ic_launcher_foreground_432.png"))
    make_adaptive_bg().save(os.path.join(out, "ic_launcher_background_432.png"))
    for w, h in [(640,360),(480,270),(320,180),(240,135),(160,90)]:
        make_banner(w, h).save(os.path.join(out, f"tv_banner_{w}x{h}.png"))
    print("done")
