import os
import glob
import re
import json
import argparse

DEFAULT_SYSTEM_PROMPT = "Tu es Aiko, une fille de 16 ans. Tu es cynique, mélancolique et tu parles en SMS avec beaucoup de kaomojis."

def clean_cdata(text):
    """Strips CDATA tags from text recursively and cleans whitespace."""
    if not text:
        return ""
    # Strip CDATA wrapper
    text = re.sub(r'<!\[CDATA\[(.*?)\]\]>', r'\1', text, flags=re.DOTALL)
    # Recursively clean if there were nested CDATAs (though unlikely with current migration)
    if "<![CDATA[" in text:
        text = clean_cdata(text)
    return text.strip()

def parse_aiko_xml(file_path):
    """Parses the Aiko XML format into a single multi-turn conversation entry."""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
            
            # Extract system prompt
            system_match = re.search(r'<system>(.*?)</system>', content, re.DOTALL)
            system_content = clean_cdata(system_match.group(1)) if system_match else DEFAULT_SYSTEM_PROMPT
            
            messages = [{"role": "system", "content": system_content}]
            
            # Extract user and assistant turns
            # We match <user> content and the following <assistant> content
            pattern = re.compile(r'<user>(.*?)</user>\s*<assistant>(.*?)</assistant>', re.DOTALL)
            turns = pattern.findall(content)
            
            for u_raw, a_raw in turns:
                u_content = clean_cdata(u_raw)
                a_content = clean_cdata(a_raw)
                
                if u_content:
                    messages.append({"role": "user", "content": u_content})
                if a_content:
                    messages.append({"role": "assistant", "content": a_content})
            
            # Only return if we have at least one back-and-forth
            if len(messages) > 1:
                return [{"messages": messages}]
            return []
    except Exception as e:
        print(f"Error parsing {file_path}: {e}")
        return []

def merge_dataset(input_dir, output_file, strip_reasoning=False):
    """Crawls input_dir for XML files and merges them into output_file (JSONL)."""
    all_files = glob.glob(os.path.join(input_dir, "**/*.xml"), recursive=True)
    merged_data = []
    
    print(f"Scanning {input_dir} for XML files...")
    for file_path in all_files:
        parsed_entries = parse_aiko_xml(file_path)
        for entry in parsed_entries:
            # Deep copy to ensure no modification leakage
            messages = [dict(m) for m in entry["messages"]]
            
            if strip_reasoning:
                for msg in messages:
                    if msg["role"] == "assistant":
                        # Remove think and emotion tags
                        msg["content"] = re.sub(r'<think>.*?</think>\n?', '', msg["content"], flags=re.DOTALL)
                        msg["content"] = re.sub(r'<emotion>.*?</emotion>\n?', '', msg["content"], flags=re.DOTALL)
                        msg["content"] = msg["content"].strip()
            
            merged_data.append({"messages": messages})
    
    if merged_data:
        with open(output_file, 'w', encoding='utf-8') as f:
            for entry in merged_data:
                f.write(json.dumps(entry, ensure_ascii=False) + '\n')
        print(f"Done! Merged {len(merged_data)} conversations into {output_file} (strip_reasoning={strip_reasoning})")
    else:
        print(f"Warning: No valid conversations found for {output_file}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Merge Aiko XML dataset into JSONL files.")
    parser.add_argument("--input", default="./dataset/aiko_fr", help="Directory containing XML files")
    
    args = parser.parse_args()
    abs_input = os.path.abspath(args.input)
    
    if not os.path.exists(abs_input):
        print(f"Error: Input directory {abs_input} does not exist.")
    else:
        # 1. Reasoning version (KEEP ALL TAGS)
        print("--- Generating Reasoning Dataset (Full) ---")
        merge_dataset(abs_input, "aiko_reasoning.jsonl", strip_reasoning=False)
        
        # 2. No-reasoning version (STRIP <think> and <emotion>)
        print("\n--- Generating Simple Dataset (Stripped) ---")
        merge_dataset(abs_input, "aiko_noreasoning.jsonl", strip_reasoning=True)
