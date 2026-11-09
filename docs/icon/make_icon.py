from PIL import Image
import sys

src_path = sys.argv[1]
out_png = sys.argv[2]
out_ico = sys.argv[3]

img = Image.open(src_path).convert("RGBA")
datas = img.getdata()

new_data = []
for r, g, b, a in datas:
    # Treat near-white pixels as background and make them transparent.
    if r > 235 and g > 235 and b > 235:
        new_data.append((r, g, b, 0))
    else:
        new_data.append((r, g, b, a))

img.putdata(new_data)
img.save(out_png, "PNG")

sizes = [(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)]
img.save(out_ico, format="ICO", sizes=sizes)
print("done")
