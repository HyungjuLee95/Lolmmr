import os
import re

target_dir = r"c:\Users\skarj\IdeaProjects\Lolmmr\frontend\src"

patterns = [
    (r'bg-\[\#0D0C1D\]', r'bg-slate-900'),
    (r'bg-\[\#161B33\]', r'bg-slate-800'),
    (r'bg-\[\#474973\]', r'bg-slate-700'),
    (r'border-\[\#0D0C1D\]', r'border-slate-900'),
    (r'border-\[\#161B33\]', r'border-slate-800'),
    (r'border-\[\#474973\]', r'border-slate-700'),
    (r'border-\[\#A69CAC\]', r'border-slate-500'),
    (r'border-\[\#F1DAC4\]', r'border-slate-300'),
    (r'text-\[\#0D0C1D\]', r'text-slate-900'),
    (r'text-\[\#161B33\]', r'text-slate-800'),
    (r'text-\[\#474973\]', r'text-slate-700'),
    (r'text-\[\#A69CAC\]', r'text-slate-400'),
    (r'text-\[\#F1DAC4\]', r'text-slate-100'),
    (r'from-\[\#A69CAC\]', r'from-slate-500'),
    (r'to-\[\#474973\]', r'to-slate-700'),
    (r'from-\[\#161B33\]', r'from-slate-800'),
    (r'to-\[\#0D0C1D\]', r'to-slate-900'),
    (r'via-\[\#F1DAC4\]', r'via-slate-300'),
    (r'from-\[\#F1DAC4\]', r'from-slate-300'),
    (r'divide-\[\#474973\]', r'divide-slate-700'),
    (r'divide-\[\#A69CAC\]', r'divide-slate-500'),
]

expanded_patterns = []
for p, rep in patterns:
    # Also handle opacity variants like bg-[#0D0C1D]/50
    # Wait, tailwind arbitrary strings are like bg-[#161B33]/80
    # re.sub won't match the /80 unless we account for it. BUT the pattern r'bg-\[\#0D0C1D\]' matches the prefix and leaves /80 intact, creating bg-slate-900/80 which is PERFECT!
    expanded_patterns.append((p, rep))

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    new_content = content
    for p, r in expanded_patterns:
        new_content = re.sub(p, r, new_content, flags=re.IGNORECASE)
        
    if new_content != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Updated {filepath}")

for root, dirs, files in os.walk(target_dir):
    for f in files:
        if f.endswith('.jsx') or f.endswith('.js'):
            process_file(os.path.join(root, f))
print("Refactoring complete.")
