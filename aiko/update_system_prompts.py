import os
import glob
import re
import random

# List of usernames to inject
USERNAMES = [
    "Alex", "Maxime", "Thomas", "Léa", "Sophie", "Camille", "Hugo",
    "Emma", "Lucas", "Chloé", "Sarah", "Antoine", "Juliette", "Paul",
    "DarkSasuke99", "xX_Kira_Xx", "Shadow_Ninja", "PtitBiscuit", "GamerDu13",
    "Raph", "Kévin", "Julien", "Marie", "Nico", "Cédric", "Laura", "Utilisateur",
    "Anonyme", "Sora", "NoobSlayer_69", "FakerFan"
]

def add_conversational_context(file_path):
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()

        # Check if already modified
        if "L'utilisateur avec qui tu parles s'appelle" in content:
            return False

        # Find the system block
        # Example: <system><![CDATA[ ... ]]></system>
        match = re.search(r'(<system><!\[CDATA\[)(.*?)(\]\]></system>)', content, re.DOTALL)
        if not match:
            print(f"Skipping {file_path}: No valid system block found.")
            return False

        prefix = match.group(1)
        system_content = match.group(2)
        suffix = match.group(3)

        random_name = random.choice(USERNAMES)
        addition = f"\nL'utilisateur avec qui tu parles s'appelle {random_name}."
        
        # Avoid double spaces/newlines if it ends with a newline already
        if system_content.endswith('\n'):
            new_system_content = system_content + f"L'utilisateur avec qui tu parles s'appelle {random_name}.\n"
        else:
            new_system_content = system_content + addition

        new_content = content[:match.start()] + prefix + new_system_content + suffix + content[match.end():]

        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        
        return True

    except Exception as e:
        print(f"Error processing {file_path}: {e}")
        return False

def main():
    base_dirs = [
        "dataset/aiko_fr",
        "dataset/aiko_fr_no_reasonning_single_message"
    ]
    
    modified_count = 0
    total_files = 0

    for base_dir in base_dirs:
        if not os.path.exists(base_dir):
            print(f"Directory {base_dir} does not exist, skipping.")
            continue
            
        xml_files = glob.glob(os.path.join(base_dir, "**/*.xml"), recursive=True)
        print(f"Found {len(xml_files)} XML files in {base_dir}")
        for fpath in xml_files:
            total_files += 1
            if add_conversational_context(fpath):
                modified_count += 1
                
    print(f"\nCompleted! Modified {modified_count} out of {total_files} files.")

if __name__ == "__main__":
    main()
