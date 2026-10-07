#!/bin/bash

# Target output file
OUTPUT="structure.txt"

echo "Ez-Touch Project Directory Structure" > "$OUTPUT"
echo "Generated on: $(date)" >> "$OUTPUT"
echo "====================================" >> "$OUTPUT"
echo "" >> "$OUTPUT"

# Use tree if available, otherwise fallback to find
if command -v tree >/dev/null 2>&1; then
    tree -I 'build|.gradle|.git|.idea|gradle-8*|*.apk|*.aar|*.class' >> "$OUTPUT"
else
    find . -maxdepth 4 \
        -not -path '*/.*' \
        -not -path './build*' \
        -not -path './app/build*' \
        -not -path './.gradle*' \
        -not -path './gradle-8*' \
        -not -path '*/.git*' \
        -not -path '*/.idea*' \
        | sort | sed -e 's/[^-][^\/]*\//  |/g' -e 's/|\([^ ]\)/|-- \1/' >> "$OUTPUT"
fi

cat "$OUTPUT"
