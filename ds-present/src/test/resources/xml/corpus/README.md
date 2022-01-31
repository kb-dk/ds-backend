# Data from various source within legacy datasets in  KB @ Cph

## The Corpus

This directory contains

1. a corpus of metadata/data for free data sets. The metadata is in
   MODS and TEI and there are also some text in TEI.
2. a transform generating a preliminary format that would help us
   normalize different data sources into a common framework.

A small sample with data in two formats but with overlapping search
characteristics (or what to call them). Here you would be able to find
a handful of persons connected to a handful of places and a handful of
digital library objects.

| File | Source | Format | Test for  |
|:-----|:-------|:-------|:----------------|
|[henrik-hertz.xml](henrik-hertz.xml)  | [letter-corpus](https://github.com/kb-dk/letter-corpus/tree/master/letter_books/001990301/001990301_000.xml) | TEI | Search for Berlin, Hertz, Brandes |
|[oersted.xml](oersted.xml) |  [letter-corpus](https://github.com/kb-dk/letter-corpus/tree/master/letter_books/002053861/002053861_X00.xml) | TEI | Search for Ørsted, Berlin, Sorø |
|[simonsen-brandes.xml](simonsen-brandes.xml) [[json](simonsen-brandes.json)] |Simonsen's Arkiv via [cop syndication](http://www5.kb.dk/cop/syndication/letters/judsam/2011/mar/dsa/subject1952/en/) | mods & RSS2 | Search for Simonsen, Brandes, København/Copenhagen |
| [albert-einstein.xml](albert-einstein.xml) [[json](albert-einstein.json)] | Simonsen's Arkiv via [cop syndication](http://www5.kb.dk/cop/syndication/letters/judsam/2011/mar/dsa/object7871) | mods & RSS2 | Search for Simonsen, Einstein, Berlin, Copenhagen |
|[tystrup-soroe.xml](tystrup-soroe.xml) [[json](tystrup-soroe.json)] | Danmark set fra luften via [cop syndication](http://www5.kb.dk/cop/syndication/images/luftfo/2011/maj/luftfoto/object322504/da/) | mods & RSS2 | Search for Sorø |
|[hvidovre-teater.xml]( hvidovre-teater.xml) [[json]( hvidovre-teater.json)] | Museet for Dansk Bladtegning via [cop syndication](http://www5.kb.dk/cop/syndication/images/billed/2010/okt/billeder/object356751) | mods & RSS2 | Search for Hvidovre, Claus Seidel, Waage Sandø, Lane Lind, Kjeld Abell |
|[homiliae-super-psalmos.xml](homiliae-super-psalmos.xml) [[json](homiliae-super-psalmos.json)]| Homiliae super Psalmos etc -- Manuscript collection via [cop syndication](http://www5.kb.dk/cop/syndication/manus/vmanus/2011/dec/ha/object71279)| mods | Just a multipage document |
| [responsa.xml](responsa.xml) [[json](responsa.json)] |[קובץ שאלות ותשובות](http://www5.kb.dk/cop/syndication/manus/judsam/2009/sep/dsh/object41158) | mods | multilingual fields (Hebrew and English). RFC5646 language tags (Like Judeo-arabic in Hebrew script |
| [work_on_logic.xml](work_on_logic.xml) [[json](work_on_logic.json)] | | | [Maimonides](https://en.wikipedia.org/wiki/Maimonides) is still widely read by students of the history of philosophy. His bibliographic authority is on http://viaf.org/viaf/100185495 |
  
## Encoding details

### Relators

Agent's (i.e., entities having a specified relation to the work
described) roles are named according to [marc relator
  list](https://www.loc.gov/marc/relators/relaterm.html) at library of
congress.



The mods name field has been translated into a field given
by its relator code. Hence
  
  
```
    <md:name type="personal" xml:lang="he">
        <md:namePart>משה בן מימון</md:namePart>
        <md:role>
            <md:roleTerm type="code">aut</md:roleTerm>
        </md:role>
    </md:name>
```

There are perhaps a handfull relators in use.

|marc relator/synonym| meaning |schema.org|
|:----|:------|:-----|
| act | actor | https://schema.org/actor |
| art | artist (in this example a cartoonist) | https://schema.org/artist |
| aut | author | https://schema.org/author |
| cre / creator | a general originator role | https://schema.org/creator |
| ctb | Contributor | https://schema.org/contributor |
| rcp | Addressee (as in recipient of a letter) |https://schema.org/recipient|
| scr | Scribe | no good mapping  https://schema.org/contributor |
| trl | Translator |https://schema.org/translator|
| pat | Patron | https://schema.org/funder |
| prt | Printer | no good mapping https://schema.org/contributor |

I propose that the search user interface have three Agent role aggregations 

* creator
* contributor
* other person

*Creator* should contain author, artist etc. *Contributor* should
contain translator, scribe etc. *Other person* should for example
contain recipient (addressee) and persons as subjects, that is a
person depicted in a photograph or a person biographed in a biography.
