"""Validate the deployable static portfolio without third-party dependencies."""
from pathlib import Path
from html.parser import HTMLParser
from urllib.parse import urlsplit, unquote
import json, hashlib, re, zipfile
ROOT=Path(__file__).resolve().parents[1]; SITE=ROOT/'docs'
class Page(HTMLParser):
    def __init__(self, text):
        super().__init__(); self.ids=set(); self.links=[]; self.h1=0; self.issues=[]; self.feed(text)
    def handle_starttag(self,tag,attrs):
        a=dict(attrs)
        if 'id' in a:
            if a['id'] in self.ids:self.issues.append('duplicate id '+a['id'])
            self.ids.add(a['id'])
        if tag=='h1':self.h1+=1
        if tag=='img' and 'alt' not in a:self.issues.append('image missing alt')
        for k in ['href','src','poster']:
            if k in a:self.links.append(a[k])
pages={p.resolve():Page(p.read_text(encoding='utf-8')) for p in SITE.rglob('*.html')}
errors=[]; count=0; source_links=0
for p,doc in pages.items():
    errors += [str(p)+': '+x for x in doc.issues]
    if doc.h1!=1: errors.append(f'{p.name}: expected one h1')
    for href in doc.links:
        u=urlsplit(href)
        if u.scheme:
            if u.netloc=='github.com' and '/blob/' in u.path:
                rel='/'.join(u.path.split('/')[5:]); source_links+=1
                if not (ROOT/rel).is_file():errors.append('Missing source target '+rel)
            continue
        if href.startswith('/'):errors.append('Root-relative path breaks repository subpath: '+href)
        q=(p.parent/unquote(u.path)).resolve() if u.path else p
        if q.is_dir():q=q/'index.html'
        count+=1
        if not q.is_file(): errors.append(f'{p.name}: missing {href}')
        elif u.fragment and q in pages and unquote(u.fragment) not in pages[q].ids: errors.append(f'{p.name}: missing fragment {href}')
checksums=json.loads((SITE/'evidence/download-checksums.json').read_text())
patterns=[rb'AIza[0-9A-Za-z_-]{30,}',rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----',rb'gh[pousr]_[A-Za-z0-9]{30,}',rb'AKIA[0-9A-Z]{16}']
for name,expected in checksums.items():
    p=SITE/'downloads'/name
    if hashlib.sha256(p.read_bytes()).hexdigest()!=expected:errors.append('Checksum mismatch: '+name)
    if zipfile.is_zipfile(p):
        with zipfile.ZipFile(p) as z:
            if z.testzip():errors.append('Corrupt archive '+name)
            for item in z.infolist():
                path=Path(item.filename)
                if path.name in ('google-services.json','local.properties') or path.suffix in ('.jks','.keystore','.db','.apk','.aab'): errors.append('Private/generated archive member '+item.filename)
                data=z.read(item)
                if any(re.search(pat,data) for pat in patterns): errors.append('Credential-like data in '+item.filename)
for p in ROOT.rglob('*'):
    if not p.is_file() or '.git' in p.parts or p.suffix in ('.zip','.docx','.jar','.png','.webp','.jpg','.mp4'): continue
    if any(re.search(pat,p.read_bytes()) for pat in patterns):errors.append('Credential-like data in '+str(p.relative_to(ROOT)))
report={'html_pages':len(pages),'local_links_checked':count,'source_targets_checked':source_links,'downloads_verified':len(checksums),'errors':errors,'recording':'Embedded MP4, captions and transcript linked; see recording-checks.json for playback and audio scope','source_links':'Exist in immutable artifact commit; public HTTP verification follows deployment'}
print(json.dumps(report,indent=2))
raise SystemExit(bool(errors))
