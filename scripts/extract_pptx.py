import zipfile, re
from xml.etree import ElementTree as ET

path = r"D:/课程/集训/小组提交材料/项目答辩PPT-第二小组/项目答辩PPT-第二小组-余夏蓉、陈玉梅.pptx"
z = zipfile.ZipFile(path)

names = sorted([n for n in z.namelist() if re.match(r'ppt/slides/slide\d+\.xml$', n)],
               key=lambda x: int(re.search(r'(\d+)', x).group(1)))
print("Number of slides:", len(names))

for n in names:
    idx = int(re.search(r'(\d+)', n).group(1))
    data = z.read(n).decode('utf-8')
    root = ET.fromstring(data)
    texts = []
    for t in root.iter('{http://schemas.openxmlformats.org/drawingml/2006/main}t'):
        if t.text:
            texts.append(t.text)
    print(f"\n===== SLIDE {idx} =====")
    print("\n".join(texts))
