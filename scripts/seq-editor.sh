#!/bin/sh
# Detect whether we're editing the todo list or a commit message.
if grep -qE '^(pick|reword|edit|squash|fixup) ' "$1"; then
  # Editing rebase todo list: turn all to reword to allow message edits
  sed -i '' 's/^pick /reword /' "$1"
else
  # Editing a commit message: normalize type scope and append issue ref
  # Normalize feat(auth): -> feat: and refactor(auth): -> refactor:
  sed -i '' 's/^feat(auth):/feat:/' "$1"
  sed -i '' 's/^refactor(auth):/refactor:/' "$1"
  # Append (#48) if not already present on the first line
  first_line=$(head -n 1 "$1")
  echo "$first_line" | grep -q '(#48)' || {
    tail_lines=$(tail -n +2 "$1")
    printf "%s (#48)\n%s" "$first_line" "$tail_lines" > "$1"
  }
fi

