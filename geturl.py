import urllib
import lxml.html

url = 'http://m.sohu.com/ch/43'

con=urllib.urlopen(url).read()
tree=lxml.html.fromstring(con)
fixed_html=lxml.html.tostring(tree,pretty_print=True)
for i in range(1,100):
    title=tree.cssselect(' > a')[i].get('href')
    print title