import os
import xml.etree.ElementTree as ET

SVG_DIR = "svg_icons"
DRAWABLE_DIR = "app/src/main/res/drawable"

if not os.path.exists(SVG_DIR):
    print(f"Directory '{SVG_DIR}' not found!")
    exit(1)

os.makedirs(DRAWABLE_DIR, exist_ok=True)

for file in os.listdir(SVG_DIR):
    if file.endswith(".svg"):
        svg_path = os.path.join(SVG_DIR, file)
        filename = os.path.splitext(file)[0]
        xml_path = os.path.join(DRAWABLE_DIR, f"{filename}.xml")

        try:
            tree = ET.parse(svg_path)
            root = tree.getroot()
            
            paths = []
            for elem in root.iter():
                if elem.tag.endswith("path"):
                    d = elem.attrib.get("d")
                    if d:
                        paths.append(d)

            if not paths:
                continue

            path_tags = "\n".join([f'    <path\n        android:fillColor="#FFFFFF"\n        android:pathData="{d}"/>' for d in paths])

            vector_xml = f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="960"
    android:viewportHeight="960">
  <group android:translateY="960">
{path_tags}
  </group>
</vector>
'''
            with open(xml_path, "w", encoding="utf-8") as f:
                f.write(vector_xml)
                
            print(f"Converted: {file} -> {xml_path}")

        except Exception as e:
            print(f"Error converting {file}: {e}")

