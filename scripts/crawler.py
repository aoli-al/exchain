from bs4 import BeautifulSoup
import requests
import re

# url = 'https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-6.html'

# page = requests.get(url)
text = open('jvms-6.html')
soup = BeautifulSoup(text, 'html.parser')

def check(s):
    print(s)
    return True

results = []

for instruction in soup.find_all('div', class_='section-execution'):
    result = instruction.find_all('h4', class_='title')
    # result = instruction.find_all('h4', class_='title')
    has_exception = False
    for item in result:
        if "Exceptions" in item.text:
            has_exception = True
    if has_exception:
        results.append(instruction.find_all(class_='titlepage')[0].div.div.h3.text.upper())

print(", ".join(results))


