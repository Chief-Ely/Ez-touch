import os
import xml.etree.ElementTree as ET

# Configuration paths
SVG_DIR = "svg_icons"
DRAWABLE_DIR = "app/src/main/res/drawable"

def convert_svg_to_xml():
    if not os.path.exists(SVG_DIR):
        print(f"Error: Directory '{SVG_DIR}' does not exist.")
        return

    os.makedirs(DRAWABLE_DIR, exist_ok=True)
    converted_count = 0

    for file in os.listdir(SVG_DIR):
        if not file.endswith(".svg"):
            continue

        svg_path = os.path.join(SVG_DIR, file)
        filename = os.path.splitext(file)[0]
        xml_path = os.path.join(DRAWABLE_DIR, f"{filename}.xml")

        try:
            tree = ET.parse(svg_path)
            root = tree.getroot()

            # Parse viewport dimensions or viewBox
            view_box = root.attrib.get("viewBox")
            viewport_width = "24"
            viewport_height = "24"

            if view_box:
                parts = view_box.strip().split()
                if len(parts) == 4:
                    viewport_width = parts[2]
                    viewport_height = parts[3]
            else:
                viewport_width = root.attrib.get("width", "24").replace("px", "").strip()
                viewport_height = root.attrib.get("height", "24").replace("px", "").strip()

            # Extract all SVG path data
            paths = []
            for elem in root.iter():
                if elem.tag.endswith("path"):
                    d = elem.attrib.get("d")
                    if d:
                        paths.append(d)

            if not paths:
                print(f"Skipped '{file}': No <path> elements found.")
                continue

            path_tags = "\n".join([
                f'    <path\n        android:fillColor="#FFFFFF"\n        android:pathData="{d}"/>'
                for d in paths
            ])

            # Check if coordinates need position normalization (e.g., Material Symbols 960 grid)
            needs_translation = False
            for d in paths:
                if "-" in d or float(viewport_height) > 100:
                    needs_translation = True
                    break

            if needs_translation:
                vector_body = f'''  <group android:translateY="{viewport_height}">
{path_tags}
  </group>'''
            else:
                vector_body = path_tags

            xml_content = f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="{viewport_width}"
    android:viewportHeight="{viewport_height}">
{vector_body}
</vector>
'''
            with open(xml_path, "w", encoding="utf-8") as f:
                f.write(xml_content)

            print(f"Successfully converted: {file} -> {xml_path}")
            converted_count += 1

        except Exception as e:
            print(f"Failed to convert '{file}': {e}")

    print(f"\nFinished! Converted {converted_count} file(s) into '{DRAWABLE_DIR}'.")

if __name__ == "__main__":
    convert_svg_to_xml()
