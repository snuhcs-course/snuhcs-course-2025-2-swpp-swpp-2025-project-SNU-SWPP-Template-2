#!/usr/bin/env python3
"""
foodlist.py - Analyze Korean food terms from menu data

This script:
1. Fetches all name_clean values from db_menus table
2. Performs short preprocessing
3. Analyzes Korean text using MeCab-ko POS tagger
4. Extracts NNG (Common Noun) tagged words
5. Counts occurrences and saves to JSON file

Usage:
  python foodlist.py
"""

import json
import os
import re
import sys
import time
from collections import Counter
from typing import Dict, List, Optional

import psycopg2
from psycopg2.extras import RealDictCursor
from dotenv import load_dotenv
from tqdm import tqdm

try:
    from konlpy.tag import Mecab
except ImportError:
    print("Error: KoNLPy is not installed. Please install it:")
    print("pip install konlpy")
    print("Note: KoNLPy requires Java JDK to be installed")
    sys.exit(1)

try:
    from langchain_openai import ChatOpenAI
    from langchain_core.prompts import PromptTemplate
    HAS_LANGCHAIN = True
except ImportError:
    print("Warning: LangChain is not installed. AI analysis will be skipped.")
    print("To enable AI analysis: pip install langchain-openai")
    HAS_LANGCHAIN = False

# Load environment variables
load_dotenv()

# Database connection parameters
DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'port': os.getenv('DB_PORT', '5432'),
    'database': os.getenv('DB_NAME', 'foodigram'),
    'user': os.getenv('DB_USER', 'postgres'),
    'password': os.getenv('DB_PASSWORD', 'postgres')
}

# OpenAI API configuration
OPENAI_API_KEY = os.getenv('OPENAI_API_KEY')
LLM_TEMPERATURE = float(os.getenv('LLM_TEMPERATURE', '0.1'))
LLM_MAX_TOKENS = int(os.getenv('LLM_MAX_TOKENS', '500'))


def get_db_connection():
    """Create and return a database connection"""
    try:
        conn = psycopg2.connect(**DB_CONFIG)
        return conn
    except Exception as e:
        print(f"Error connecting to database: {e}")
        sys.exit(1)


def short_preprocess(text: str) -> str:
    """
    Short preprocessing to normalize food terms and protect compound words:
    """
    if not text:
        return ""
    
    # Normalize katsu variations
    text = re.sub(r'돈카츠|돈까스|카츠', '돈가스', text)
    
    return text


def extract_compound_words(text: str) -> List[str]:
    """
    Extract known compound food words that might be incorrectly segmented by morphological analysis
    """
    if not text:
        return []
    
    # Define compound food words that should be kept as single units
    compound_patterns = [
        r'후라이드',           # fried (chicken)
        r'사이드',             # side (dish)
        r'에이드',             # ade (drink)
        r'양념치킨',           # seasoned chicken  
        r'간장치킨',           # soy sauce chicken
        r'크리스피',           # crispy
        r'바베큐',             # barbecue
        r'핫윙',               # hot wings
        r'닭똥집',           # chicken gizzard
        r'닭발',               # chicken feet
        r'허니머스타드',       # honey mustard
        r'갈릭',               # garlic
        r'치즈볼',             # cheese ball
        r'모짜렐라',           # mozzarella
        r'페퍼로니',           # pepperoni
        r'마르게리타',         # margherita
        r'콤비네이션',         # combination
        r'슈프림',             # supreme
        r'하와이안',           # hawaiian
        r'불고기',             # bulgogi
        r'김치찌개',           # kimchi stew
        r'된장찌개',           # soybean paste stew
        r'순두부찌개',         # soft tofu stew
        r'부대찌개',           # army stew
        r'순대국',             # sundae soup
        r'만두국',             # dumpling soup
        r'감자탕',             # pork backbone stew
        r'삼겹살',             # pork belly
        r'목살',               # pork neck
        r'갈비살',             # ribs
        r'치킨까스',           # chicken cutlet
        r'돈가스',             # pork cutlet (already normalized)
        r'함박스테이크',       # hamburg steak
        r'스파게티',           # spaghetti
        r'파스타',             # pasta
        r'라자냐',             # lasagna
        r'리조또',             # risotto
        r'피자',               # pizza
        r'햄버거',             # hamburger
        r'치즈버거',           # cheeseburger
        r'불고기버거',         # bulgogi burger
        r'새우버거',           # shrimp burger
        r'핫도그',             # hot dog
        r'샌드위치',           # sandwich
        r'토스트',             # toast
        r'크로아상',           # croissant
        r'베이글',             # bagel
        r'와플',               # waffle
        r'쉬림프',             # shrimp
        r'휘낭시에',           # financier
        r'피낭시에',           # financier (alternate spelling)
        r'아보카도',         # avocado
    ]
    
    found_compounds = []
    text_lower = text.lower()
    
    for pattern in compound_patterns:
        matches = re.findall(pattern, text_lower)
        found_compounds.extend(matches)
    
    return found_compounds


def initialize_llm() -> Optional[ChatOpenAI]:
    """Initialize ChatOpenAI model"""
    if not HAS_LANGCHAIN or not OPENAI_API_KEY:
        return None
    
    try:
        model_name = os.getenv('OPENAI_MODEL', 'gpt-4o-mini')
        
        return ChatOpenAI(
            openai_api_key=OPENAI_API_KEY,
            model_name=model_name,
            temperature=LLM_TEMPERATURE,
            max_tokens=LLM_MAX_TOKENS
        )
    except Exception as e:
        print(f"Error initializing ChatOpenAI: {e}")
        return None


def analyze_food_word_with_ai(word: str, llm: ChatOpenAI) -> Optional[Dict]:
    """
    Analyze a food word using AI to determine food classification, allergens, and taste profile
    """
    if not llm:
        return None
    
    prompt_template = PromptTemplate(
        input_variables=["word"],
        template="""
Analyze the Korean food word "{word}" and provide the following information in JSON format:

1. Classification: Determine if this word represents an ingredient, cooking style, or menu name. Return "ingredient", "cooking_style", "menu_name", or "other".

2. Allergen Analysis: Check if this food item typically contains any of these allergens:
   - Milk
   - Eggs  
   - Fish (e.g., bass, flounder, cod)
   - Crustacean shellfish (e.g., crab, lobster, shrimp)
   - Tree nuts (e.g., almonds, walnuts, pecans)
   - Peanuts
   - Wheat
   - Soybeans
   - Sesame
   
   Return as a list of allergen names that are typically present.

3. Taste Profile: Rate the typical taste characteristics on a scale of 0.0 to 1.0:
   - Sweet: 0.0 = not sweet, 1.0 = very sweet
   - Salty: 0.0 = not salty, 1.0 = very salty
   - Spicy: 0.0 = not spicy, 1.0 = very spicy

Return ONLY a valid JSON object with this exact structure:
{{
  "classification": "ingredient|cooking_style|menu_name|other",
  "allergens": ["list", "of", "allergens"],
  "taste_profile": {{
    "sweet": 0.0,
    "salty": 0.0,
    "spicy": 0.0
  }}
}}

Word to analyze: {word}
"""
    )
    
    try:
        formatted_prompt = prompt_template.format(word=word)
        result = llm.invoke(formatted_prompt)
        
        # Extract content from ChatOpenAI response
        if hasattr(result, 'content'):
            response_text = result.content.strip()
        else:
            response_text = str(result).strip()
        
        # Parse JSON response - handle markdown code blocks
        try:
            # Remove markdown code blocks if present
            if response_text.startswith('```json'):
                response_text = response_text.replace('```json', '').replace('```', '').strip()
            elif response_text.startswith('```'):
                response_text = response_text.replace('```', '').strip()
            
            analysis = json.loads(response_text)
            return analysis
        except json.JSONDecodeError:
            print(f"Failed to parse JSON for word '{word}': {response_text}")
            return None
            
    except Exception as e:
        print(f"Error analyzing word '{word}': {e}")
        return None


def analyze_high_frequency_words(word_counts: Dict[str, int], min_count: int = 10) -> Dict[str, Dict]:
    """
    Analyze words with count >= min_count using AI
    """
    if not HAS_LANGCHAIN or not OPENAI_API_KEY:
        print("Skipping AI analysis - LangChain or OpenAI API key not available")
        return {}
    
    # Initialize LLM
    llm = initialize_llm()
    if not llm:
        print("Failed to initialize LLM - skipping AI analysis")
        return {}
    
    # Filter words with high frequency
    high_freq_words = {word: count for word, count in word_counts.items() if count >= min_count}
    
    if not high_freq_words:
        print(f"No words found with count >= {min_count}")
        return {}
    
    print(f"\nAnalyzing {len(high_freq_words)} words with count >= {min_count} using AI...")
    
    ai_analyses = {}
    
    for word in tqdm(high_freq_words.keys(), desc="AI Analysis"):
        analysis = analyze_food_word_with_ai(word, llm)
        if analysis:
            ai_analyses[word] = {
                'count': high_freq_words[word],
                'classification': analysis.get('classification', 'unknown'),
                'allergens': analysis.get('allergens', []),
                'taste_profile': analysis.get('taste_profile', {'sweet': 0.0, 'salty': 0.0, 'spicy': 0.0})
            }
        
        # Small delay to avoid rate limiting
        time.sleep(0.1)
    
    return ai_analyses


def initialize_mecab() -> Optional[Mecab]:
    """Initialize KoNLPy Mecab tagger"""
    try:
        # Try to initialize KoNLPy Mecab
        tagger = Mecab()
        return tagger
    except Exception as e:
        print(f"Error initializing KoNLPy Mecab: {e}")
        print("\nTroubleshooting:")
        print("1. Ensure Java JDK is installed:")
        print("   sudo apt-get install default-jdk")
        print("2. Install KoNLPy:")
        print("   pip install konlpy")
        print("3. Check Java installation:")
        print("   java -version")
        return None


def extract_target_words(text: str, tagger: Mecab) -> List[str]:
    """
    Extract NNG, VA, and XR tagged words from text using KoNLPy Mecab
    NNG: 일반명사 (Common Noun)
    VA: 형용사 (Adjective) 
    XR: 어근 (Root)
    """
    if not text or not tagger:
        return []
    
    try:
        # Parse text with KoNLPy Mecab - returns list of (word, pos) tuples
        pos_tags = tagger.pos(text)
        words = []
        
        # Extract words tagged as NNG, VA, or XR
        target_pos = {'NNG', 'VA', 'XR'}
        for word, pos in pos_tags:
            if pos in target_pos:
                # Filter out single characters and very short words
                if len(word) >= 2:
                    words.append(word)
        
        return words
    except Exception as e:
        print(f"Error parsing text '{text}': {e}")
        return []


def fetch_menu_names(cursor) -> List[str]:
    """Fetch all name_clean values from db_menus table"""
    query = """
    SELECT name_clean
    FROM db_menus 
    WHERE name_clean IS NOT NULL AND TRIM(name_clean) != ''
    ORDER BY id;
    """
    
    try:
        cursor.execute(query)
        results = cursor.fetchall()
        return [row['name_clean'] for row in results]
    except Exception as e:
        print(f"Error fetching menu names: {e}")
        return []


def analyze_food_terms() -> Dict[str, int]:
    """
    Main analysis function:
    1. Fetch menu names from database
    2. Preprocess text
    3. Extract NNG words with MeCab-ko
    4. Count occurrences
    """
    # Connect to database
    conn = get_db_connection()
    
    try:
        with conn.cursor(cursor_factory=RealDictCursor) as cursor:
            print("Fetching menu names from database...")
            menu_names = fetch_menu_names(cursor)
            
            if not menu_names:
                print("No menu names found in database")
                return {}
            
            print(f"Found {len(menu_names)} menu names")
            
    finally:
        conn.close()
    
    # Initialize KoNLPy Mecab
    print("Initializing KoNLPy Mecab...")
    tagger = initialize_mecab()
    if not tagger:
        return {}
    
    # Process menu names
    print("Processing menu names...")
    all_nng_words = []
    
    for menu_name in tqdm(menu_names, desc="Analyzing menus"):
        # Short preprocessing
        preprocessed = short_preprocess(menu_name)
        
        # Extract compound words first (to preserve them as single units)
        compound_words = extract_compound_words(preprocessed)
        all_nng_words.extend(compound_words)
        
        # Remove compound words from text before POS analysis to avoid duplicates/fragments
        text_for_pos = preprocessed
        for compound in compound_words:
            # Use case-insensitive replacement
            text_for_pos = re.sub(re.escape(compound), ' ', text_for_pos, flags=re.IGNORECASE)
        
        # Extract target words (NNG, VA, XR) from remaining text
        target_words = extract_target_words(text_for_pos, tagger)
        all_nng_words.extend(target_words)
    
    # Count occurrences
    print("Counting word occurrences...")
    word_counts = Counter(all_nng_words)
    
    # Convert to regular dict and sort by count
    sorted_word_counts = dict(word_counts.most_common())
    
    print(f"Found {len(sorted_word_counts)} unique target words (NNG, VA, XR)")
    print(f"Total target word occurrences: {sum(sorted_word_counts.values())}")
    
    return sorted_word_counts


def save_results(word_counts: Dict[str, int], ai_analyses: Dict[str, Dict] = None, output_file: str = "food_terms_nng.json"):
    """Save word counts and AI analysis to JSON file"""
    try:
        # Prepare output data
        output_data = {
            "metadata": {
                "description": "Korean food terms extracted from menu names using hybrid approach",
                "extraction_methods": [
                    "Pattern matching for compound food words (e.g., 후라이드, 양념치킨)",
                    "POS tagging for NNG (일반명사), VA (형용사), XR (어근)"
                ],
                "total_unique_words": len(word_counts),
                "total_occurrences": sum(word_counts.values()),
                "preprocessing_applied": ["돈카츠/돈까스/카츠 -> 돈가스"],
                "pos_tags": ["NNG (일반명사/Common Noun)", "VA (형용사/Adjective)", "XR (어근/Root)"],
                "compound_words_preserved": True,
                "min_word_length": 2,
                "ai_analysis_included": ai_analyses is not None,
                "ai_analysis_count": len(ai_analyses) if ai_analyses else 0
            },
            "word_counts": word_counts
        }
        
        # Add AI analysis if available
        if ai_analyses:
            output_data["ai_analysis"] = ai_analyses
        
        # Save to JSON file
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(output_data, f, ensure_ascii=False, indent=2)
        
        print(f"Results saved to {output_file}")
        
        # Print top 20 words
        print("\nTop 20 most common target words (NNG, VA, XR):")
        print("-" * 40)
        for i, (word, count) in enumerate(list(word_counts.items())[:20], 1):
            ai_info = ""
            if ai_analyses and word in ai_analyses:
                analysis = ai_analyses[word]
                classification = analysis.get('classification', 'unknown')
                allergens = analysis.get('allergens', [])
                taste = analysis.get('taste_profile', {})
                
                ai_info = f" | {classification}"
                if allergens:
                    ai_info += f" | allergens: {', '.join(allergens)}"
                if any(taste.values()):
                    taste_str = ", ".join([f"{k}:{v:.1f}" for k, v in taste.items() if v > 0])
                    if taste_str:
                        ai_info += f" | taste: {taste_str}"
            
            print(f"{i:2d}. {word:<15} ({count:,} times){ai_info}")
            
    except Exception as e:
        print(f"Error saving results: {e}")


def main():
    """Main function"""
    print("="*60)
    print("KOREAN FOOD TERMS ANALYSIS")
    print("="*60)
    print("Analyzing food terms using hybrid approach:")
    print("- Pattern matching for compound words (후라이드, 양념치킨, etc.)")
    print("- POS tagging for NNG, VA, XR words")
    print()
    
    # Analyze food terms
    word_counts = analyze_food_terms()
    
    if not word_counts:
        print("No results to save")
        return
    
    # Perform AI analysis on high-frequency words
    ai_analyses = analyze_high_frequency_words(word_counts, min_count=10)
    
    # Save results
    save_results(word_counts, ai_analyses)
    
    print("\n Analysis completed successfully!")


if __name__ == "__main__":
    main()