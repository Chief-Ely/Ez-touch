import os

EXCLUDED_NAMES = {
    ".git",
    ".gradle",
    ".idea",
    "build",
    ".cxx",
    "captures",
    ".externalNativeBuild",
    "gradlew",
    "gradlew.bat",
    "local.properties",
    "structure.txt",
    "export_structure.py",
    "__pycache__"
}

def print_directory_tree(folder_path, prefix="", is_last=True, output_lines=None):
    if output_lines is None:
        output_lines = []

    folder_name = os.path.basename(folder_path) or folder_path
    
    if folder_name in EXCLUDED_NAMES:
        return output_lines

    if prefix:
        connector = "└── " if is_last else "├── "
        line = f"{prefix}{connector}{folder_name}"
    else:
        line = folder_name

    if os.path.isdir(folder_path):
        line += "/"
    
    output_lines.append(line)

    if os.path.isdir(folder_path):
        try:
            entries = os.listdir(folder_path)
        except PermissionError:
            return output_lines

        filtered = [e for e in entries if e not in EXCLUDED_NAMES]
        filtered.sort(key=lambda name: (not os.path.isdir(os.path.join(folder_path, name)), name.lower()))

        for i, child_name in enumerate(filtered):
            child_path = os.path.join(folder_path, child_name)
            child_is_last = (i == len(filtered) - 1)
            new_prefix = prefix + ("    " if is_last else "│   ") if prefix else ""
            print_directory_tree(child_path, new_prefix, child_is_last, output_lines)

    return output_lines

if __name__ == "__main__":
    output_lines = print_directory_tree(".")
    with open("structure.txt", "w", encoding="utf-8") as f:
        f.write("\n".join(output_lines) + "\n")
    print("Project structure exported to structure.txt")
