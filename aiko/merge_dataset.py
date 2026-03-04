import os
import glob
import re
import json
import argparse

def parse_aiko_xml(file_path):
    """Parses the Aiko XML format into a list of conversation dictionaries."""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
            
            # Extract all pairs if there are multiple user/assistant blocks in one file
            system_matches = re.finditer(r'<system>(.*?)</system>', content, re.DOTALL)
            user_matches = re.finditer(r'<user>(.*?)</user>', content, re.DOTALL)
            think_matches = re.finditer(r'<think>(.*?)</think>', content, re.DOTALL)
            emotion_matches = re.finditer(r'<emotion>(.*?)</emotion>', content, re.DOTALL)
            assistant_matches = re.finditer(r'<assistant>(.*?)</assistant>', content, re.DOTALL)
            
            systems = [m.group(1).strip() for m in system_matches]
            users = [m.group(1).strip() for m in user_matches]
            thinks = [m.group(1).strip() for m in think_matches]
            emotions = [m.group(1).strip() for m in emotion_matches]
            assistants = [m.group(1).strip() for m in assistant_matches]
            
            results = []
            # We zip based on the longest list but usually they should match
            count = max(len(users), len(assistants))
            
            # Use a default fallback or the first system prompt found in the file
            default_system = systems[0] if systems else ""
            
            for i in range(count):
                s = systems[i] if i < len(systems) else default_system
                u = users[i] if i < len(users) else ""
                t = thinks[i] if i < len(thinks) else ""
                e = emotions[i] if i < len(emotions) else ""
                a = assistants[i] if i < len(assistants) else ""
                
                # Construct the text field, omitting categories that are empty
                text_parts = []
                if s: text_parts.append(f"<system>{s}</system>")
                if u: text_parts.append(f"<user>{u}</user>")
                if t: text_parts.append(f"<think>{t}</think>")
                if e: text_parts.append(f"<emotion>{e}</emotion>")
                if a: text_parts.append(f"<assistant>{a}</assistant>")
                
                full_prompt = "\n".join(text_parts)
                results.append({"text": full_prompt})
            
            return results
    except Exception as e:
        print(f"Error parsing {file_path}: {e}")
        return []

def merge_dataset(input_dir, output_file):
    """Crawls input_dir for XML files and merges them into output_file (JSONL)."""
    all_files = glob.glob(os.path.join(input_dir, "**/*.xml"), recursive=True)
    merged_data = []
    
    print(f"Scanning {input_dir} for XML files...")
    for file_path in all_files:
        parsed_entries = parse_aiko_xml(file_path)
        if parsed_entries:
            merged_data.extend(parsed_entries)
    
    # Save to JSONL
    with open(output_file, 'w', encoding='utf-8') as f:
        for entry in merged_data:
            f.write(json.dumps(entry, ensure_ascii=False) + '\n')
    
    print(f"Done! Merged {len(merged_data)} examples into {output_file}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Merge Aiko XML dataset into a single JSONL file.")
    parser.add_argument("--input", default="./dataset/aiko_fr_instruct", help="Directory containing XML files")
    parser.add_argument("--output", default="aiko_dataset_fr_instruct.jsonl", help="Output JSONL filename")
    
    args = parser.parse_args()
    
    # Ensure we are in the right directory or handle paths relatively
    abs_input = os.path.abspath(args.input)
    abs_output = os.path.abspath(args.output)
    
    if not os.path.exists(abs_input):
        print(f"Error: Input directory {abs_input} does not exist.")
    else:
        merge_dataset(abs_input, abs_output)
