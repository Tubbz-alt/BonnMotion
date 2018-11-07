%%  Natdin.BST   Vers. [2.4f5]  6.11.04   mit doi-, urn-, url-Funktionen
%%  K.F.Lorenzen  (C)    email:  klaus.lorenzen@bui.haw-hamburg.de
%%  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
%%  Mit diesem BibTex-style werden deutsche Literaturverzeichnisse nach
%%  DIN 1505, Teil 2 und 3 formatiert. Die label im Literaturverzeichnis sind
%%  vom Typ 'Verfasser - Jahr' und entsprechen den Zitatformen im Text. Eine
%%  Beschreibung des Stils mit Beispielen findet sich unter der
%%  URL  http://www.fh-hamburg.de/pers/Lorenzen/bibtex.
%%  NATDIN.BST muss zusammen mit NATBIB.STY von Patrick W. Daly und meiner
%%  Konfiguration NATBIB.CFG aufgerufen werden. Alle drei Dateien sind
%%  vorzugsweise in den ...\bibtex\bst - Pfad zu stellen.
%%
%%  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
%%  version 0.99c for BibTeX versions 0.99c or later, LaTeX2e version
%%  Copyright (C) 1985, all rights reserved.
%%  Copying of this file is authorized only if either
%%  (1) you make absolutely no changes to your copy, including name, or
%%  (2) if you do make changes, you name it something other than
%%  natdin.bst, natbib.sty
%%  This restriction helps ensure that all standard styles are identical.
%%
%%  KFL, Jan 2004
%%

ENTRY
  { address      %%  Verlagsort
    author       %%  persoenlicher Urheber eines Werkes
    booktitle    %%  a) Gesamttitel eines mehrbaendigen Werkes
%                %%  b) Titel des Sammelwerks, das einzelne selbstaendige
%                %%     Beitraege mit eigenem Titel enthaelt (->incollection)
    chapter      %%  Kapitel in einem Buch (Monographie)
    doi          %%% Digital Object Identifier
    edition      %%  Auflagevermerk; bei selbst. elektron. Dok. == Version
    editor       %%  Persoenl.Herausgeber oder Koerperschaftlicher Herausgeber
    howpublished %%  beliebiger Verlegervermerk: von wem, wo
    institution  %%  Institution, die e.Verlagsfreie Veroeffentlichung betreibt
    isbn         %%  Standardnr fuer Buecher
    issn         %%  - " -    :      Zeitschriften u. Serien
    journal      %%  Titel einer Zeitschrift
    key          %%  Zusaetzlich vergebener Sortierschluessel, mitunter notwend.
    lastchecked  %%  Online Abruf einer Internetquelle ( GRAY ); 22.1.2004
    month        %%  naehere Bestimmung des Erscheinungsjahres (-> macro 's)
    note         %%  freies Eingabefeld fuer zusaetzliche Informationen
    number       %%  Mehrfachbedeutung in Abhaengigkeit vom Eingabetyp
    organization %%  a) Name der Organisation/des Organisators e. Tagung,Konferenz
%                %%  b) Name einer Firma/Gesellschaft, die ein ->manual herausgab
    pages        %%  Umfangsangaben, meist Seitenzahlen
    publisher    %%  Verlag
    school       %%  Hochschule/Universitaet, die eine Dipl.-Arb./Dissertation veroeff.
    series       %%  Titel e.Reihe, in der ein best. Buchtitel erschienen ist
    title        %%  Titel einer (namentlich gekennzeichneten) Veroeffentlichung
    type         %%  Zusatzfeld z.Kennzeichnung e.besonderen Publikationstyps
    url          %%%  URL-Feld ( Uniform Resource Locator )
    urn          %%   URN-Feld ( Uniform Resource Name )
    volume       %%  a) Zaehlung bei einem mehrbaendigen Werk (-> book)
%                %%  b) Jahrgang einer Zeitschrift (-> article
    year         %%  Erscheinungsjahr
  }
  {}
  { label extra.label sort.label short.list }

INTEGERS { output.state before.all mid.sentence after.sentence after.block }

%% die folg. BOOLE'sche VAR steuern d. Ausg. ": " nach Urheber-Feldern
%%    und ". - " vor ISBN oder Anmerkungen (NOTE)

INTEGERS { colon.after  period.dash }

%% nur bei INBOOK, wenn ein Kapitel oder Anhang als erstes ausgegeben wird
%% 28.2.2004
INTEGERS { after.firstblock }

FUNCTION {init.state.consts}
{ #0 'before.all :=
  #1 'mid.sentence :=
  #2 'after.sentence :=
  #3 'after.block :=
  #4 'colon.after :=
  #5 'period.dash :=
  #6 'after.firstblock :=
}
INTEGERS { zahl  lang }

STRINGS { h s t u v }

%% neue DIN-Funktion, 16/2/94

FUNCTION {output.nonnull}
{ 's :=
  output.state after.block =
      { add.period$  write$
        newline$
        "\newblock " write$
      }
      { output.state before.all =
          { write$ }
          {  output.state colon.after =
               { ": " * write$
                 newline$
                 "\newblock " write$
               }
               { output.state period.dash =
                   { ". -- " *  write$
                     newline$
                     "\newblock " write$
                   }
                   { output.state mid.sentence =
                       { ", " * write$ }
                       { output.state after.sentence =
                           { " " * write$ }
                           { output.state after.firstblock =
                               { add.period$  write$
                                 newline$
                                 "\newblock " write$
                               }
                               { write$
                                 newline$
                                 "\newblock " write$
                               }
                             if$
                            }
                          if$
                       }
                     if$
                   }
                 if$
               }
             if$
           }
         if$
       after.block 'output.state :=
      }
    if$
    s
 }

FUNCTION {output}
{ duplicate$ empty$
    'pop$
    'output.nonnull
  if$
}

FUNCTION {output.check}
{ 't :=
  duplicate$ empty$
    { pop$ "empty " t * " in " * cite$ * warning$ }
    'output.nonnull
  if$
}


FUNCTION {new.block}
{ output.state before.all =
    'skip$
    { after.block 'output.state := }
  if$
}

FUNCTION {format.url.vorher}
{ url empty$
    { "" }
   %% evtl. vor url noch new.block!
    { " \url{" url * "}" * } %%   evtl. "URL" oder "<...>"
  if$
}

%%5.4.04
FUNCTION {format.url}
{ urn missing$
     { doi missing$
          { url empty$
             { "" }
             %% evtl. vor url noch new.block!
             { " \url{" url * "}" * } %%   evtl. "URL" oder "<...>"
           if$
          }
          { " \url{http://dx.doi.org/" doi * "}" *
             %%  { " \url{http://dx.medra.org/" doi * "}" *
          }
        if$
      }
      { " \url{http://nbn-resolving.de/urn/resolver.pl?urn=" urn * "}" *
      }
    if$
}

FUNCTION {format.doi}
{ doi empty$
    { "" }
    { new.block " \url{http://dx.doi.org/" doi * "}" *
     %%      { new.block " \url{http://dx.medra.org/" doi * "}" *
    }
  if$
}

INTEGERS { nameptr namesleft numnames }

FUNCTION {output.bibitem}
{ newline$
  "\bibitem[" write$
  label write$%       das label enth"alt nur die Autoren ohne Jahresangabe
  "]{" write$
  cite$ write$
  "}" write$
  newline$
  "\newblock \\" write$%%   nur Zeilenumbruch
  newline$
  ""
  before.all 'output.state :=
}
%% PWD

FUNCTION {fin.entry}   %%$$$  nach DIN neu 16/2/94
{ write$
  newline$
}

FUNCTION {set.period.dash}    %% Wenn ein ". - " die Satzteile trennen soll.!
{ output.state before.all =
    'skip$
    { period.dash  'output.state := }
  if$
}

%%   neu 16/2/94
%%   prueft, ob PAGES, ISBN- oder NOTE-Feld vh. ist und setzt dann ". - " davor.

FUNCTION {set.period.dash.check}
{ empty$
    'skip$
    'set.period.dash
  if$
}

FUNCTION {set.colon.after}   %%$$$ Wenn ein ": " d. Satzteile trennen soll!
{ output.state before.all =
    'skip$
    { colon.after 'output.state  := }
  if$
}

%% neu / alt 17/2/94          Wenn ein " " die Satzteile trennen soll.!
FUNCTION {new.sentence}
{ output.state before.all =
     'skip$
     { after.sentence 'output.state := }
  if$
}

%% neu 17/2/94                 Wenn ein ", " die Satzteile trennen soll.!
FUNCTION { part.of.sentence }
{ output.state before.all =
     'skip$
     { mid.sentence 'output.state := }
  if$
}


FUNCTION {not}
{   { #0 }
    { #1 }
  if$
}

FUNCTION {and}
{   'skip$
    { pop$ #0 }
  if$
}

FUNCTION {or}
{   { pop$ #1 }
    'skip$
  if$
}

FUNCTION {new.sentence.checka}
{ empty$
    'skip$
    'new.sentence
  if$
}

FUNCTION {field.or.null}
{ duplicate$ empty$
    { pop$ "" }
    'skip$
  if$
}

FUNCTION {emphasize}
{ duplicate$ empty$
    { pop$ "" }
    { "\emph{" swap$ * "}" * }
  if$
}

STRINGS { fkt }

FUNCTION { capitalize }
 { duplicate$ empty$
    { pop$ "" }
    { "\textsc{" swap$ * "}" * }
  if$
}

%%$$$ DIN-Quellenangabe : spezieller unselbst. Teil ist erschienen "In: "
%%                        dem bibliogr. selbst. Werk, z.B. Zeitschrift, Buch

%% 13.11.2003 nach e. Idee v. H. Baumann
FUNCTION {article.in.journal}
{ duplicate$ empty$
    { pop$ "" }
    { author missing$ title missing$ and
            { emphasize }%% wenn ein Zs-Heft als ganzes zitiert wird
            { emphasize "{In: }" swap$ * }
          if$
     }
  if$
}

%% Vorschlag von H.Lueddecke, um Adelspraedikate beim Sortieren
%% nach den(m) Vornamen aufzufuehren. Lo, 2/11/94  Abweichend v. DIN !!!
FUNCTION {format.names}
{ 's :=
  "" 'u  :=
  #1 'nameptr :=
  s num.names$ 'numnames :=
  numnames 'namesleft :=
    { namesleft #0 > }
    {
       %vorher, Lue      s nameptr "{vv~}{ll}" format.name$ 't :=
       s nameptr "{ll}" format.name$ 't :=
       t capitalize 't :=
       %vorher,Lue      s nameptr "{, ff}" format.name$ 'u :=
       s nameptr "{, ff}{ vv}" format.name$ 'u :=
       u  text.length$  'lang :=
       #1 'zahl :=
       "" 'v :=
         {  zahl  lang  <  }
         {  u  zahl  #1  substring$  "~"  =
               {  v  ""  =
                     {  u  #1  zahl  #1  -  substring$  'v :=  }
                     'skip$
                  if$
                  v  u  zahl  #2  substring$  *  "."  *  'v :=  }
               'skip$
            if$
            zahl  #1  +  'zahl :=  }
      while$
      v  ""  =
         {  u  'v :=  }
         'skip$
      if$
%%    der string fkt enthaelt " (Hrsg.)", wenn Editorfeld nicht leer ist
      t  v  *  fkt  *  't :=  %% Komma nach Nachnamen wird oben erledigt!
                              %% t enthaelt nun d. formatierten Nnamen, Vnamen
      nameptr #1 >
         { namesleft #1 >
             { ", " * t * }
             { numnames #2 >
                  { " " * }
                  'skip$
               if$
%%                                %% n. schindle's hinweis 12/1/96 erweitert
               t "\textsc{others}" = t "\textsc{others} (Hrsg.)" = or
                    { "et~al." * } %% Geschmackssache, waehle eins von beiden
%%                    { " [u.~a.]" * }%%   13/2/94
                    { " und " * t * }
%%                    { " / " * t * }
               if$
               }
            if$  %% Ende der namesleft-Pruefung
          }
            't
           %% hierdurch wird bei jed. Schleifendurchgang das sich komplet-
           %% tierende Zwischen-Namensergebnis wieder auf den stack gelegt

       if$  %%  Ende der nameptr-Pruefung

      nameptr #1 + 'nameptr :=
      namesleft #1 -  'namesleft :=
    }
  while$  %%  Ende von { namesleft #0 > } ganz oben
  "" 'fkt :=  %% fkt wird zurueckgesetzt
}

%%$$$ geaendert 14/2/94

FUNCTION {format.authors}
{ author empty$
    { "" }
    { author format.names }
  if$
}

%%$$$ geaend. 20/2/94 Anpassung an DIN, wonach Autor + Hrsg. zusammen vorkom-
%%                    men duerfen.!!

FUNCTION {format.editors}
{ editor empty$
   { author empty$
       { "empty author and editor in " cite$ * warning$ "" }
       'skip$
     if$
   }
   { author empty$
        { " (Hrsg.)" 'fkt :=
          editor format.names %%%%*  %%% Test 26.11.03
        }
        { " (Hrsg.)" 'fkt :=
          " ; " * editor format.names *
        }
      if$
   }
 if$
 }

%% Lo, 12/5/99 neue Funktion fuer proceedings, misc usw.

FUNCTION { format.editors.organization }
{ organization empty$
    'skip$
    { type$ "misc" =
        { organization }%%  vorher war noch * davor, 25.1.04
        { * " ; " * organization " (Veranst.)" *}
      if$
    }
  if$
}

%%  Sonderfall: Herausgeber bei Typ incollection, 21/2/94
FUNCTION {format.ed.incoll}
{ editor empty$
     { "" }
     { " (Hrsg.)" 'fkt :=
       editor format.names
     }
  if$
}


FUNCTION {format.title}
{ title empty$
    { "" }
    { title }  %% Text so wie er dasteht im Feld title
  if$
}

%% wg MISC-Funktion, 10.12.03
FUNCTION {format.number}
{ number empty$
    { "" }
    { number " " * }  %% Text so wie er im Feld number steht plus " "
  if$
}


%% wg MISC-Funktion u. BOOKLET-Fkt., 29.12.03
FUNCTION {format.type}
{ type empty$
    { "" }
    { type " " * }  %% Text wie er dasteht; Schutzrecht, Norm etc.
  if$
}

%% in BOOKLET-Fkt., 22.1.04
FUNCTION {format.resource.type}
{ type empty$
    { "" }
    { url empty$
       { "" }
       { "[" type * "]" * }
     if$
    }
  if$
}


FUNCTION {n.dashify}
{ 't :=
  ""
    { t empty$ not }
    { t #1 #1 substring$ "-" =
        { t #1 #2 substring$ "--" = not
            { "--" *
              t #2 global.max$ substring$ 't :=
            }
            {   { t #1 #1 substring$ "-" = }
                { "-" *
                  t #2 global.max$ substring$ 't :=
                }
              while$
            }
          if$
        }
        { t #1 #1 substring$ *
          t #2 global.max$ substring$ 't :=
        }
      if$
    }
  while$
}


%%  Auflagenvermerke gibt man komplett, einschliesslich Abkuerzungen in
%%  das Feld edition ein: ---> EDITION= { 3., erw. und verb. Aufl. }
%%  oder fremdsprachlich:      EDITION= { 2nd edition }

FUNCTION {format.edition}
{ edition empty$
    { "" }
    { edition }
  if$
}




%% geaendert 24/2/94
%%FUNCTION {format.date}
%%{ year empty$
%%    { month empty$
%%        { "" }
%%        { "there's a month but no year in " cite$ * warning$
%%          month
%%        }
%%      if$
%%    }
%%    { month empty$  %% b. Buechern nur Jahr, ohne Monat ausgeb. im Impressum
%%          { year }
%%          { month " " * year * }
%%      if$
%%    }
%%  if$
%%  extra.label *
%%}
%%13.11.2003 weg n. e. Idee v. H.B.  extra.label * %     nach PWD, month-Problem?
%%

FUNCTION {format.date}%%%%   n. PWD 3.12.03
{ year duplicate$ empty$
    { "empty year in " cite$ * warning$
       pop$ "" }
     'skip$
  if$
  month empty$
     'skip$
    { type$ "book" =%%      bei Buechern nur das Jahr ausgeben!
      type$ "inbook" =
       OR
         'skip$
         { month " " * swap$ * }
      if$
     }
   if$
%%% extra.label *   % d. Extrabuchst. soll nicht beim Ersch.-Jahr auftauchen!
}
%%% ueberarbeiten
FUNCTION {form.edit.or.date}
{ edition empty$ year empty$ and
    'skip$
    { "\,[Vers.:" new.sentence}
  if$
  edition empty$
    { * format.date "]" * }
    { year empty$
       { * "{\,}" * edition * "]" * }
       { * "{\,}" * edition * ", " *
         format.date "]" *
       }
      if$
    }
  if$
 }

%%   22.1.04 Versionsangaben werden nicht geklammert!
FUNCTION {format.edition.or.date}
{ edition empty$ year empty$ and
    { "" }
    { edition empty$
        { type #-1 #4 substring$ "mail" =
          type #1 #4 substring$ "Mail" =
          or
             { "Gesendet: "                            %%new.sentence
               format.date *
               after.firstblock 'output.state :=
             }
             { "\,Version:\,"                          %%new.sentence
               format.date *
               after.firstblock 'output.state :=
             }
           if$
        }
        { year empty$
            { "\,Version:\," edition * }
            { "\,Version:\," edition * ", " *
              format.date *
            }
          if$
        }
      if$
    }
  if$
}

%%     das sog. Impressum
FUNCTION {format.address.publisher.year}
{  publisher empty$
      { address empty$
          { year empty$
              { "" }
              { url empty$
                   { year }
                   { "" }
                 if$
              }
            if$
          }
          { "there's an address but no publisher in " cite$ * warning$
              address ", " * format.date *
          }
        if$
      }
      {  url empty$%%%%  wenn es URL gibt wird nur die service-provider Adresse
%%%                      ausgegeben, die im publisher-Feld steht
          { address empty$
               { year empty$
                   { "neither address nor publication date in " cite$ * warning$
                      publisher
                   }
                   { publisher ", " * format.date * }
                 if$
               }
               { year empty$
                   { address " : " * publisher * }
                   { address " : " * publisher * ", " * format.date * }
                 if$
               }
             if$
          }
          { publisher }
        if$
      }
    if$
}

%% wg BOOKLET-/MISC-/UNPUBLISHED-Fkt., 29.12.03
FUNCTION {format.howpublished}
{ url missing$ urn missing$ doi missing$ AND AND
    { howpublished empty$
        { address empty$
            { type empty$
              { "" }
              { type #-1 #4 substring$ "mail" =
                  { "(gesendet: " new.sentence
                    format.date * ")" *
                  }                           
                  { "" }
                if$
               }
             if$
             format.date *%% 25.6.04
            }
            { address ", " * format.date * }
          if$
        }
        { address empty$
            { howpublished ", " * format.date * }
            { address " : " * howpublished * ", " * format.date * }
          if$
        }
      if$
    }
    { howpublished empty$
        { "" }
        { howpublished }
      if$
    }
  if$
}

%% fuer booklet, 22.1.04
FUNCTION {format.lastcheck.vorher}
{ lastchecked empty$
    { url empty$
        { type empty$
            { skip$ }
            { type set.period.dash.check
              type output
            }
          if$
        }
        { skip$ }
      if$
    }
    { type$ "misc" =
        { part.of.sentence
          lastchecked "Abruf: " swap$ * output
        }
        { lastchecked set.period.dash.check
          lastchecked "Online Abruf: " swap$ * output
          note output
        }
      if$
    }
  if$
}

%% 1.Febr.04
FUNCTION {format.lastcheck.still}
{   url empty$
%%                    { skip$ }
      {  type empty$
           { skip$ }
           { type set.period.dash.check
             type output
           }
         if$
      }
      { lastchecked empty$
           { skip$ }
           { type$ "misc" =
               { part.of.sentence
                 lastchecked "Abruf: " swap$ * output
               }
               { lastchecked set.period.dash.check
                 lastchecked "Online Abruf: " swap$ * output
                 note output
               }
             if$
            }
          if$
        }
      if$
}

%%% 6.4.04
FUNCTION {format.lastchecked}
%%  dann gibt es einen URL oder URN oder DOI!
{    lastchecked empty$
           { skip$ }
           { type set.period.dash.check
             type output
             part.of.sentence
             lastchecked "Abruf: " swap$ * output
           }
        if$
 }

%%% 5.4.04
FUNCTION {format.lastcheck.or.type}
{   url empty$%%              dann gibt es einen URN oder DOI!
      { lastchecked empty$
           { skip$ }
           { type$ "booklet" =
                { type set.period.dash.check
                  type output
                  part.of.sentence
                  lastchecked "Abruf: " swap$ * output
                }
                { part.of.sentence
                  lastchecked "Abruf: " swap$ * output
                }
              if$
            }
          if$
       }
       { lastchecked empty$
           { skip$ }
           { type$ "misc" =
               { part.of.sentence
                 lastchecked "Abruf: " swap$ * output
               }
               { type set.period.dash.check
                 type output
                 part.of.sentence
                 lastchecked "Abruf: " swap$ * output
               }
             if$
            }
          if$
        }
      if$
}


FUNCTION {format.btitle}
{ title emphasize
}

FUNCTION {tie.or.space.connect}
{ duplicate$ text.length$ #3 <
    { "~" }
    { " " }
  if$
  swap$ * *
}

%%FUNCTION {either.or.check}
%%{ empty$
%%    'pop$
%%    { "can't use both " swap$ * " fields in " * cite$ * warning$ }
%%  if$
%%}


FUNCTION {format.btitle.vol}
{ number empty$
   { series empty$
     { volume empty$
        { title emphasize }
        { title emphasize ". Bd." * volume tie.or.space.connect }
       if$
     }
     { volume empty$
        { title emphasize }%% ein Buch, das zusaetzl. SERIES=Reihentitel besitzt
    %% jetzt kommt d. Fall des mehrbaendigen Werkes mit Gesamttitel=SERIES
    %% Zaehlung=VOLUME und Bandtitel=TITLE;
        { series emphasize ". Bd." * volume tie.or.space.connect
                                 ": " * "{\emph{" * title * "}}" * }
       if$
     }
     if$%% series-test
   }
   { title emphasize }%% wenn number ex., dann immer title-Feld
  if$%% Ende number-test
}

%%$$$ neu 16/2/94
%%   Serien- / Reihentitel werden im Feld series gespeichert. Weist die
%%   Serie eine Zaehlung der Einzeltitel auf, gibt man entweder nach DIN alles
%%   in das Feld series so ein: ---> TITEL DER SERIE  NR. (der Vorlage) <---
%%   z. B.  SERIES= { Mensch und Computer 12 }.
%%   [ Die Nummer der Vorlage darf auch durch " ; " abgesetzt werden. ]
%%   oder:
%%   man gibt die Zaehlung in das Feld NUMBER ein, z.B. NUMBER = {12}.
%%   Achtung!!
%%   Bei mehrbaendigen Werken steht d. Gesamttitel im Feld SERIES und die
%%   Bandzaehlung im Feld VOLUME; NUMBER darf dann nicht besetzt sein!
%%   Anderenfalls liegt ein Erfassungsfehler vor, da sich Reihe u. mehrbd.
%%   Werk gegenseitig ausschliessen.

FUNCTION {format.series.number.din}
{ volume empty$
    { number empty$
          { series empty$
               { "" }%%   Ausstieg mit Nullstring
               { "(" series * ")" * } %% d. Seriennr koennte auch gleich hier
                                      %% im SERIES-Feld miterfasst werden
            if$
          }
          { series empty$
               { "(" number tie.or.space.connect ")" *
                 "there's a number but no series in " cite$ * warning$
               }
               { "(" series * number tie.or.space.connect ")" * }
            if$
          }
       if$
     }
     { series empty$
          { "" }
          { type$ "proceedings" =  %% Sonderfall, es darf VOLUME und NUMBER ex. !
                { number empty$
                    { "(" series * ")" * }
                    { "(" series * number tie.or.space.connect ")" * }
                  if$
                }
                { "" }%%   Ausstieg mit Nullstring, s. Kommentar
            if$
          }%% bei gezaehlten Reihen MUSS die Reihennr. im Feld NUMBER stehen!
       if$ %% wenn also d. Feld VOLUME nicht leer ist, dann liegt ausser bei
           %% Typ PROCEEDINGS falsche
      }     %% Erfassung vor und es erfolgt d. Ausstieg mit d. Nullstring!
  if$
}

%% seltener Fall bei MISC: Ausgabe einer Serie; die Nummer der Serie muss
%%  in SERIES miterfasst werden  16/6/99
FUNCTION {format.misc.series}
{ series empty$
    { "" }
    { "(" series * ")" * }
  if$
}

%%  4.4.2004
FUNCTION { format.doi.urn }
{ urn empty$
     { doi empty$
          { "" }
          { "DOI" doi n.dashify tie.or.space.connect }
       if$
     }
     { "URN" urn n.dashify tie.or.space.connect }
  if$
}




%%$$$ neu, 18/3/94
FUNCTION { format.isbn.issn }
{ isbn empty$
     { issn empty$
          { "" }
          { "ISSN" issn n.dashify tie.or.space.connect }
       if$
     }
     { "ISBN" isbn n.dashify tie.or.space.connect }
  if$
}

%%$$$ geaendert, 21/2/94  gibt Seitenzahl bei BOOK-Typ und verwandten T. aus
FUNCTION {format.pages.book}
{ pages empty$
    { "" }
%%    { ". -- " pages n.dashify tie.or.space.connect " S" *}%% stillgel., 17/12/95
    { "" pages n.dashify tie.or.space.connect " S" *} %% 17/12/95
  if$
}

%%$$$  alle anderen Seitenang. zB. Zeitschrft., INBOOK usw. a la Orig.,
%%  bei Internetquellen nur den Inhalt von pages ausgeben, 10.2.2004
FUNCTION {format.pages}
{ pages empty$
    { "" }
    { url empty$
        { "S." pages n.dashify  tie.or.space.connect }
        { pages }
      if$
    }
  if$
}

%%$$$ Angaben v. Jahrgang, Jahr, Heftnr., Seiten bei Artikel-Typ
%%    14/3/94

FUNCTION {format.vol.year.num.pages}
{ volume field.or.null
    year empty$
      { "there's a volume but no year in " cite$ * warning$ }
      { " (" year * ")" * * }
    if$
    month empty$
      'skip$
      { ", " month * * }
    if$
    number empty$
      'skip$
      { ", Nr. " number * * }
    if$
    pages empty$
      'skip$
      { duplicate$ empty$
        { pop$ "" }%% da pages leer, wird nur "" auf stack gelegt
        { title missing$%%  in dem Fall gibt es nur "journal"-Feld  neu 14/6/99
            { ", " format.pages.book * *}%% wenn Zs. als Ganzes aufgenommen ist
            { ", " format.pages * *}
          if$
        }
      if$
    }
  if$
}

%% 28.2.04
FUNCTION {format.chapter.inbook}
{ duplicate$  empty$
    { pop$ "empty chapter in " cite$ * warning$ }
    { type empty$
         { "\emph{Kapitel\/} " swap$ tie.or.space.connect }
         { type " " * swap$ * }%%  wenn keine bes. Abschnittsform gen. werden soll,
                 %%  koennte e. kl. Zwischenraum gewaehlt werden, z.B. " \, "
       if$
     }
  if$
  %%after.firstblock 'output.state :=
 }

%% geaendert wg url-Feld 3.2.2004
FUNCTION {format.chapter.pages}
{ chapter empty$
    'format.pages
    { type empty$
        { "Kapitel " }
        { url empty$
           { type }
           { "Kapitel " }
         if$
        }
      if$
      chapter tie.or.space.connect
      pages empty$
        'skip$
        { ", " * format.pages * }
      if$
    }
  if$
}

%%$$$  geaendert 21/2/94
FUNCTION {format.in.ed.booktitle.din}
{ booktitle empty$
    { "" }
    { editor empty$
        { volume empty$
            { "{In: }" booktitle emphasize * }%% n. Belieben fettes In:
            { "{In: }" booktitle emphasize *  %%      - " -
              " Bd." volume tie.or.space.connect *
            }
          if$
        }
        { volume empty$
            { "{In: }" format.ed.incoll * ": " * booktitle emphasize * }
            { "{In: }" format.ed.incoll * ": " * booktitle emphasize *
              " Bd." volume tie.or.space.connect *
            }
          if$
        }
      if$
    }
  if$
}

%% geaendert 6/4/04 wg elektron. Dokumente
FUNCTION {format.thesis.type}
{ type empty$
    'skip$
    { type #1 #6 substring$ "Online" =%%%  d.i. Online Ressource
        'skip$
        { pop$
          type
        }
     if$
    }
  if$
}

%% geaendert 23/2/94    i.Orig. wird zuerst die number, dann der type getestet
FUNCTION {format.tr.number.din}
{ type empty$
     { number empty$
         { " -- Forschungsbericht" }   %%  bei Minimalangaben besser ohne "."!
         { "(" number tie.or.space.connect "). -- Forschungsbericht" * }
       if$
     }
     { number empty$
         { " -- " type * }             %%  bei Minimalangaben besser ohne "."!
         { "(" number tie.or.space.connect "). -- " * type * }
       if$
     }
  if$
}


FUNCTION {format.article.crossref}
{ key empty$
    { journal empty$
        { "need key or journal for " cite$ * " to crossref " * crossref *
          warning$
          ""
        }
        { "{In: }{\emph " journal * "}" * }%% n. Belieben fettes In:
      if$
    }
    { "{In: }" key * }%%    s.o. fettes In:
  if$
  " {\textbf{\cite{" * crossref * "}" * "}" * "}" * ", " * format.pages *
}

%%geaendert 7/3/94 und noch einmal durch Lueddecke, s.o.
FUNCTION {format.crossref.editor}
%vorher,Lue  { editor #1 "{vv~}{ll}" format.name$ " (Hrsg.)" *
 { editor #1 "{ll}" format.name$ " (Hrsg.)" *
   editor num.names$ duplicate$
   #2 >
%%    { pop$ " [u.~a.~]" * }
    { pop$ " et~al." * }
    { #2 <
        'skip$
        { editor #2 "{ff }{vv }{ll}{ jj}" format.name$ "others" =
%%            { " [u.~a.]" }
            { " et~al." * }
            { ", " * editor #2 "{vv~}{ll}" format.name$ * " (Hrsg.)" * }
          if$
        }
      if$
    }
  if$
}


FUNCTION {format.inbk.vol.title}
{ volume empty$
     { " In: " }
     { title empty$
        { " In: Bd." volume tie.or.space.connect 
                " von " *
        }
        { "In: Bd." volume tie.or.space.connect ": " * title emphasize *
                     " (" * year * ") in " *
        }
      if$
     }
   if$
 }


%% 6.11.2004  selten gebrauchte Fkt.
FUNCTION {format.book.crossref}
{ type$ "inbook" =
%   { volume empty$
%     { " In: " }
%     {  " In: Bd." volume tie.or.space.connect "{\emph{" * title * ", }}" * 
%              year * ", von " *
%     }
%   if$
%   }
   { format.inbk.vol.title }
%% wenn es ein BOOK ist, dann
   { volume empty$
       { "empty volume in " cite$ * "'s crossref of " * crossref * warning$
         " "
       }
       { ". -- Bd." volume tie.or.space.connect
          " von " *
       }
     if$
    }
  if$
  editor empty$
  editor field.or.null author field.or.null =
  or
    { key empty$
        { series empty$
            { "need editor, key, or series for " cite$ * " to crossref " *
              crossref * warning$
              "" *
            }
            { "" * }%% dadurch kommt nach der Band Nr. gleich das label 2/6/99
%%              { "{\emph{" * series * "}} {\textbf{siehe}} " * }
          if$
        }
        { key * }
      if$
    }
    { "" * }%% danach kommt gleich das label; Lo 2/6/99
%%      { format.crossref.editor * } vor 2/6/99 = Hrsg.-Vermerk
  if$
  "{\textbf{\cite{" * crossref * "}" * "}" * "}" *
}

FUNCTION {format.incoll.inproc.crossref}
{ editor empty$
    editor field.or.null author field.or.null =
    or
     { key empty$
        { booktitle empty$
            { "need editor, key, or booktitle for " cite$ * " to crossref " *
              crossref * warning$
              ""
            }
            { "{In: }{\emph " booktitle * "}" * }%%
          if$
        }
        { "{In: }" }%% ohne EDITOR koennte KEY (wenn vh.) genommen werden;
%%                       der wird dann aber noch zusaetzlich am Schluss zur
%%                       label-Produktion genommen, d.h. er waere 2x da!
%%        { "{In: }" key * }%% frueher war es so
      if$
     }
    { "{In: }" }%% Lo, 24/5/99 es sieht der reine Bezug (Referenz) besser aus!
%%    { "{In: }{\emph{" booktitle * "}" * "}" * }%%  Variante des Bezugs
%%    { "{In: }" format.crossref.editor * }%%     --  "  --
  if$
  "{\textbf{\cite{" * crossref * "}" * "}" * "}" * ", " * format.pages *%
              %% das fette label, dahinter die Seitenangabe Lo 23/2/99
}

%%geaendert 6.4.2004
FUNCTION {article}
{ output.bibitem
  format.authors "author" output.check
  set.colon.after
  format.title "title" output.check
  crossref missing$
    { journal article.in.journal output.nonnull
      new.sentence
      format.vol.year.num.pages output
    }
    { format.article.crossref output.nonnull }
  if$
  format.url output
  format.lastchecked
  note set.period.dash.check
  note output
  doi set.period.dash.check
  urn set.period.dash.check
  format.doi.urn output
  issn set.period.dash.check
  format.isbn.issn output
  fin.entry
}

%%$$$   geaendert, 20/2/94
FUNCTION {book}
{ output.bibitem
  author empty$
    { format.editors "author and editor" output.check }
    { format.authors format.editors output.nonnull }
  if$
  set.colon.after
  crossref missing$
    { format.btitle.vol "title" output.check }
    { format.btitle "title" output.check }
  if$
  format.edition output
  format.address.publisher.year "publisher" output.check
  new.sentence
  crossref missing$
    { format.series.number.din output
      pages set.period.dash.check
      format.pages.book output
    }
    { format.book.crossref output.nonnull
      pages set.period.dash.check
      format.pages.book output
    }
  if$
  note set.period.dash.check
  note output
  format.doi output
  format.url output
  new.block
  isbn set.period.dash.check
  format.isbn.issn output
  fin.entry
}

%%% fuer Internetquellen mit URL, 22.1.04
FUNCTION {booklet}
{ output.bibitem
  author empty$
    { format.editors "author and editor" output.check }
    { format.authors format.editors output.nonnull }
  if$
  set.colon.after
  format.btitle "title" output.check
  url empty$ doi empty$ urn empty$ and and
    { format.edition output }
    { format.edition.or.date output
      format.url output
    }
  if$
  format.howpublished output
  new.sentence
  format.series.number.din output
  pages set.period.dash.check
  format.pages.book output
  format.lastchecked
  doi set.period.dash.check
  urn set.period.dash.check
  format.doi.urn output
  note set.period.dash.check
  note output
  format.isbn.issn output
  fin.entry
}

%% geaendert 23/2/94
FUNCTION {inbook}
{ output.bibitem
%% zuerst der unselbst. Teil eines Buches
    chapter format.chapter.inbook output.nonnull
%%   -------- jetzt kommt der bibliogr. selbst. Teil
      crossref missing$
      { author empty$
          { format.editors "\,{In:\,}" swap$ * "author and editor" output.check }
          { format.authors "\,{In:\,}" swap$ * output.nonnull }
        if$
        author empty$ editor empty$ AND
           { before.all 'output.state := }
           { set.colon.after }
         if$
      format.btitle.vol "title" output.check
      format.edition output
      format.address.publisher.year "publisher" output.check
      new.sentence
      format.series.number.din output
      isbn set.period.dash.check
      format.isbn.issn output
      part.of.sentence
      format.pages output
      note set.period.dash.check
      note output 
    }
    { format.book.crossref output.nonnull
      part.of.sentence
      format.pages output
      note set.period.dash.check
      note output 
    }
  if$
  fin.entry
}

%%  geaendert 21/2/94 und wieder 9.4.2004
FUNCTION {incollection}
{ output.bibitem
  format.authors "author" output.check
  set.colon.after
  format.title "title" output.check
  url empty$ doi empty$ urn empty$ and and
    { skip$ }
    { format.edition.or.date output }
  if$
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
  crossref missing$
    { format.in.ed.booktitle.din "booktitle" output.check
      format.edition output
      format.address.publisher.year "publisher" output.check
      new.sentence
      format.series.number.din output %%$$$ 7/3/94
      format.lastchecked
      doi set.period.dash.check
      urn set.period.dash.check
      format.doi.urn output
%%%%      note set.period.dash.check
%%%%      note output
      isbn set.period.dash.check
      issn set.period.dash.check
      format.isbn.issn output
      part.of.sentence
      format.chapter.pages "pages" output.check
      format.url output
      note set.period.dash.check
      note output 
    }
    { format.incoll.inproc.crossref output.nonnull
%%%%%%%%%%%
      format.lastchecked
      doi set.period.dash.check
      urn set.period.dash.check
      format.doi.urn output
%%%%      note set.period.dash.check
%%%%      note output
      isbn set.period.dash.check
      issn set.period.dash.check
      format.isbn.issn output
      %%%%%%%part.of.sentence
      %%%%%%%format.chapter.pages "pages" output.check
      format.url output
      note set.period.dash.check
      note output 
    }
  if$
  fin.entry
}

%%  geaendert 22/2/94, 15/11/96 (Hinweis v. Alin Shindun, Uni Siegen)
FUNCTION {inproceedings}
{ output.bibitem
  format.authors "author" output.check
  set.colon.after
  format.title "title" output.check
  crossref missing$
    { format.in.ed.booktitle.din "booktitle" output.check
      address empty$
        { organization new.sentence.checka
          organization output
          part.of.sentence
          format.address.publisher.year output
        }
        { format.address.publisher.year "publisher" output.check }
      if$
      new.sentence
      series empty$ %%neu nach Hinweis v. Alin Shindun, 15/11/96
        'skip$
        { format.series.number.din output }
      if$
%%%%      note set.period.dash.check
%%%%      note output
      isbn set.period.dash.check
      issn set.period.dash.check
      format.isbn.issn output
      part.of.sentence
      format.pages output
      note set.period.dash.check
      note output 
    }
    { format.incoll.inproc.crossref output.nonnull
%%%%      note set.period.dash.check
%%%%      note output
      isbn set.period.dash.check
      issn set.period.dash.check
      format.isbn.issn output
      note set.period.dash.check
      note output 
    }
  if$
  fin.entry
}

FUNCTION {conference} { inproceedings }%% nach Patashnik, wg US-Kompatibilitaet

%% geaendert, 11/6/99
FUNCTION {manual}
{ output.bibitem
  author empty$
    { organization empty$
        { title empty$
            'skip$
            {format.btitle "title" output.check }
          if$
        }
        { organization output.nonnull
          set.colon.after
          format.btitle "title" output.check
        }
      if$
    }
    { format.authors output.nonnull
      set.colon.after
      format.btitle "title" output.check
    }
  if$
  format.edition "edition" output.check
  author empty$
    { organization empty$
        { address field.or.null * %%output
          part.of.sentence
        }
        'skip$
      if$
    }
    { address field.or.null ": " *  organization * output
      part.of.sentence
    }
  if$
  format.date output
  pages set.period.dash.check
  format.pages.book output
  note set.period.dash.check
  note output
  format.doi output
  format.url output
  new.block
  fin.entry
}

%%  MASTERSTHESIS  KFL, 17/2/94
%%  geaendert KFL, 17/2/94, 6.4.2004
%%  Standard = "Diplomarbeit", sonst TYPE="anderer Typ"
%%  z.B. TYPE={Dissertation}, TYPE={Diss.}, TYPE={Habil.}, TYPE={Magisterarb.}
FUNCTION {mastersthesis}
{ output.bibitem
  format.authors "author" output.check
  set.colon.after
  format.btitle "title" output.check
  address output
  part.of.sentence
  school "school" output.check
  part.of.sentence
  "Diplomarbeit" format.thesis.type output.nonnull
  part.of.sentence
  format.date "year" output.check
  url empty$ doi empty$ urn empty$ and and
    { skip$ }
    { format.url output }
  if$
  format.lastchecked
  doi set.period.dash.check
  urn set.period.dash.check
  format.doi.urn output
  pages set.period.dash.check
  format.pages.book output
  note set.period.dash.check
  note output
  fin.entry
}

FUNCTION {phdthesis} %%  {mastersthesis}% ist identisch bis auf Standardwert, s.o.
{ output.bibitem
  format.authors "author" output.check
  set.colon.after
  format.btitle "title" output.check
  address output
  part.of.sentence
  school "school" output.check
  part.of.sentence
  "Diss." format.thesis.type output.nonnull % koennte auch `Dissertation' sein
  part.of.sentence
  format.date "year" output.check
  url empty$ doi empty$ urn empty$ and and
    { skip$ }
    { format.url output }
  if$
  format.lastchecked
  doi set.period.dash.check
  urn set.period.dash.check
  format.doi.urn output
  pages set.period.dash.check
  format.pages.book output
  note set.period.dash.check
  note output
  fin.entry
}

%% das type-Feld uebernimmt eine wichtige Steuerfunktion:
FUNCTION {misc}
{ output.bibitem
   type missing$ not
       { type duplicate$ #1 #4 substring$ "Norm" =
           { " " *
             format.number * output
             new.sentence
             format.date output
             title empty$
               { skip$ }
               { add.period$ new.sentence }
             if$
             format.btitle "title" output.check
             note set.period.dash.check
             note output
           }
           { duplicate$ #1 #6 substring$ "Schutz" =
                { " " * format.number * output
                  new.sentence
                  "(" * format.date ")" * output
                  add.period$ new.sentence
                  format.authors add.period$ output
                  note output
                }
            %%  wenn irgendein anderer Typ eingetragen ist
                { pop$
                  title empty$
                     { note empty$
                         { url empty$
                             { "there's no reasonable field in " cite$ warning$
                                pop$ ""
                             }
                             { format.url output }%%%  das waere e. reine URL
                           if$
                         }
                         { note " " * output.nonnull }
                       if$
                     } %%% bis jetzt hatte es keinen Titel gegeben
                     { author empty$ %%% wenn title vh., sollte auch e. Urheber da sein
                          { editor empty$
                               { organization empty$  %%%% eigentlich "howpublished"
                                   { skip$ }
                                   { format.editors.organization output.nonnull
                                     set.colon.after
                                   }
                                 if$
                               }
                               { format.editors format.editors.organization
                                 output.nonnull set.colon.after
                               }
                             if$
                          }
                          { format.authors format.editors output.nonnull
                            set.colon.after
                          }
                        if$
                 format.btitle output.nonnull
                 url empty$
                   { format.edition output
                     format.howpublished output}
                   { format.howpublished output
                     format.edition.or.date output %%27.1.04 wg Ort:Verl. Jahr
                     format.url output
%%                     format.edition.or.date output }
                   }
                 if$
                 new.sentence
                 format.misc.series output
                 type set.period.dash.check
                 type output
                 format.lastcheck.or.type
                 note output
                }
              if$
            }
         if$
          }
        if$
       }%%%  Ende des Zweigs, wenn ein type ex.
       {%%%  wenn kein type-Feld ex.
         title empty$
             { note empty$
                 { url empty$
                     { "there's no relevant field in " cite$ warning$
                       pop$ ""
                     }
                     { format.url output }%%%  das waere e. reine URL
                   if$
                 }
                 { note " " * output.nonnull
                 %%  format.url format.date output
                 }
               if$
             } %%% bis jetzt hatte es keinen Titel gegeben
             { author empty$ %%% wenn title vh., sollte auch e. Urheber da sein
                   { editor empty$
                        { organization empty$  %%%% eigentlich "howpublished"
                             { skip$ }
                             { format.editors.organization output.nonnull
                               set.colon.after
                             }
                           if$
                        }
                        { format.editors format.editors.organization
                          output.nonnull set.colon.after
                        }
                      if$
                   }
                   { format.authors format.editors output.nonnull
                     set.colon.after
                   }
                 if$
              format.btitle output.nonnull
              url empty$
                { format.edition output
                  format.howpublished output }
                { format.howpublished output
                  format.url output
                  format.edition.or.date output }
              if$
              new.sentence
              format.misc.series output
              note set.period.dash.check
              note output
          }
        if$%%%  ende d. Abfrage, ob title vh. ist

      }
    if$%%%  ende von type missing (ganz oben)
 fin.entry
}

%% geaendert Lo, 23/5/99
FUNCTION {proceedings}
{ output.bibitem
    editor empty$
      { organization empty$
           { "empty organization and editor in " cite$ * warning$ }
    %%%       { skip$ }
           { organization " (Veranst.)" * output }
        if$
      }
      { format.editors format.editors.organization output.nonnull }
    if$
  set.colon.after
  format.btitle "title" output.check
  volume empty$
    { skip$ } %%%%26/11 vorher= { "" output.nonnull }
    { "{\textnormal{Bd.}}" volume tie.or.space.connect emphasize "volume" output.check }
  if$
  format.address.publisher.year "publisher" output.check
  new.sentence
  format.series.number.din output.nonnull
  pages set.period.dash.check
  format.pages.book output
  note set.period.dash.check
  note output
  isbn set.period.dash.check
  issn set.period.dash.check
  format.isbn.issn output
  fin.entry
}

%% geaendert 23/2/94 auch fuer Firmenschriften u."a. zu benutzen
FUNCTION {techreport}
{ output.bibitem
  author empty$
    { format.editors "author and editor" output.check }
    { format.authors  format.editors output.nonnull }
  if$
  set.colon.after
  format.title "title" output.check
  institution new.sentence.checka
  institution empty$
     'skip$
     { " / " institution *  output.nonnull }
  if$
  format.address.publisher.year output
  number new.sentence.checka
  format.tr.number.din "number" output.check
%%  new.sentence
  pages set.period.dash.check
  format.pages.book output
  note "note" output.check
  format.doi output
  format.url output
  new.block
  isbn set.period.dash.check
  issn set.period.dash.check
  format.isbn.issn output
  fin.entry
}


FUNCTION {unpublished} {misc}%% author, title, note muessen sein! howpublished
%%                           %% entfaellt natuerlich

FUNCTION {default.type} { misc }

MACRO {jan} {"Januar"}

MACRO {feb} {"Februar"}

MACRO {mar} {"M{\^^b a}rz"}
%% nach Bernd Raichle, Febr. 1999

MACRO {apr} {"April"}

MACRO {mai} {"Mai"}

MACRO {may} {"Mai"}

MACRO {jun} {"Juni"}

MACRO {jul} {"Juli"}

MACRO {aug} {"August"}

MACRO {sep} {"September"}

MACRO {okt} {"Oktober"}

MACRO {oct} {"Oktober"}

MACRO {nov} {"November"}

MACRO {dez} {"Dezember"}

MACRO {dec} {"Dezember"}

%%$$$  stillgelegte Beispiele fuer den Gebrauch von Kuerzeln (hier Zs-Titel).

%%MACRO {acmcs} {"ACM Computing Surveys"}

%%MACRO {acta} {"Acta Informatica"}

READ

FUNCTION {sortify}
{ purify$
  "l" change.case$
}

INTEGERS { len }

FUNCTION {chop.word}
{ 's :=
  'len :=
  s #1 len substring$ =
    { s len #1 + global.max$ substring$ }
    's
  if$
}

%%PWD
FUNCTION {format.lab.names}
{ 's :=
  s #1 "{vv~}{ll}" format.name$
  s num.names$ duplicate$
  #2 >
%%    { pop$ " u.~a." * }%%  Geschmackssache
     { pop$ " et~al." * }%% 4/6/99 so war's
    { #2 <
        'skip$
        { s #2 "{ff }{vv }{ll}{ jj}" format.name$ "others" =
%%              { " u.~a." * }
            { " et~al." * }
%%            { " u. " * s #2 "{vv~}{ll}" format.name$ * }
%%          { " und " * s #2 "{vv~}{ll}" format.name$ * }
          { " / " * s #2 "{vv~}{ll}" format.name$ * }
          if$
        }
      if$
    }
  if$
}


FUNCTION {author.key.label}
{ author empty$
    { key empty$
        { cite$ #1 #3 substring$ }
        { key }
%%        { key #3 text.prefix$ }
      if$
    }
    { author format.lab.names }
  if$
}

FUNCTION {author.editor.key.label}
{ author empty$
    { editor empty$
        { key empty$
            { cite$ #1 #3 substring$ }
            { key }
%%            { key #3 text.prefix$ }
          if$
        }
        { editor format.lab.names }
      if$
    }
    { author format.lab.names }
  if$
}

FUNCTION {author.key.organization.label}
{ author empty$
    { key empty$
        { organization empty$
            { cite$ #1 #3 substring$ }
            { "The " #4 organization chop.word #4 text.prefix$ }
          if$
        }
        { key }
      if$
    }
    { author format.lab.names }
  if$
}

FUNCTION {editor.key.organization.label}
{ editor empty$
    { key empty$
        { organization empty$
            { cite$ #1 #3 substring$ }
%%            { "The " #4 organization chop.word *} %% Lo, 8/2/98
%%            { "The " #4 organization chop.word organization } %% Lo, 26/1/98
            { "The " #4 organization chop.word #4 text.prefix$ } %% so war's
          if$
        }
        { key }
%%        { key #3 text.prefix$ }
      if$
    }
    { key empty$%%  das key-feld soll zwangsweise das label machen! Lo,18/5/99
        { editor format.lab.names }
        { key }%%das ganze label
      if$
    }
  if$
}

FUNCTION {calc.short.authors}
{ type$ "book" =
  type$ "inbook" =
  or
    'author.editor.key.label
    { type$ "proceedings" =
        'editor.key.organization.label
        { type$ "manual" =
            'author.key.organization.label
            'author.key.label
          if$
        }
      if$
    }
  if$
  'short.list :=
}

FUNCTION {calc.label}
{ calc.short.authors
  short.list
  " "
   *
%%%  year field.or.null purify$ #-1 #5 substring$%% wenn year zus. Marke bekommt
%%!!!!!!!!!%%  year field.or.null purify$ #1 #4 substring$%% nur die ersten 4 Ziffern!
%% nur die letzten 4 Ziffern!%%  year field.or.null purify$ #-1 #4 substring$
%% Einschub a la PWD, 21.6.04

%%  year duplicate$ empty$
%%     short.list key field.or.null = or
%%       { pop$ "" }
%%       'skip$
%%     if$
  year field.or.null
  *
  'label :=
label sortify 'sort.label :=
}

FUNCTION {sort.format.names}
{ 's :=
  #1 'nameptr :=
  ""
  s num.names$ 'numnames :=
  numnames 'namesleft :=
    { namesleft #0 > }
      { nameptr #1 >
        { "   " * }
        'skip$
      if$
%     s nameptr "{vv{ } }{ll{ }}{  ff{ }}{  jj{ }}" format.name$ 't :=
%
%     Zeile geaendert, damit die Namenszusaetze wie von, de usw nach deutscher
%     Norm richtig einsortiert werden.   27.10.94 Lueddecke
%
      s nameptr "{ll{ }}{  ff{ }}{ vv{ }}{  jj{ }}" format.name$ 't :=
      nameptr numnames = t "others" = and
        %%{ "[u.~a.]" * }
        { "et al" * }% Geschmackssache
        { t sortify * }
      if$
      nameptr #1 + 'nameptr :=
      namesleft #1 - 'namesleft :=
      }
  while$
}

FUNCTION {sort.format.title}
{ 't :=
  "A " #2
    "An " #3
      "Der " #4
        "Die " #4
          "Das " #4
            "Ein " #4
              "Eine " #5
                  "The " #4 t chop.word
               chop.word
             chop.word
           chop.word
         chop.word
       chop.word
    chop.word
  chop.word
  sortify
  #1 global.max$ substring$
}

FUNCTION {author.sort}
{ author empty$
    { key empty$
        { "to sort, need author or key in " cite$ * warning$
          ""
        }
        { key sortify }
      if$
    }
    { author sort.format.names }
  if$
}

FUNCTION {author.editor.sort}
{ author empty$
    { editor empty$
        { key empty$
            { "to sort, need author, editor, or key in " cite$ * warning$
              ""
            }
            { key sortify }
          if$
        }
        { editor sort.format.names }
      if$
    }
    { author sort.format.names }
  if$
}

FUNCTION {author.organization.sort}
{ author empty$
    { organization empty$
        { key empty$
            { "to sort, need author, organization, or key in " cite$ * warning$
              ""
            }
            { key sortify }
          if$
        }
        { "The " #4 organization chop.word sortify }
      if$
    }
    { author sort.format.names }
  if$
}

%%  neu vom 22.11.2003, KFL
FUNCTION {editor.organization.sort}%%das key-Feld soll z. Sortieren ben. werden
{ key empty$
    { editor empty$
       { organization empty$
            { "to sort, need editor, organization, or key in " cite$ * warning$
             ""
            }
            { "The " #4 organization chop.word sortify }
          if$
        }
        { editor sort.format.names }
      if$
    }
    { key sortify }
  if$
}

FUNCTION {presort}
{ calc.label
  sort.label
  "    "
  *
  type$ "book" =
  type$ "inbook" =
  or
    'author.editor.sort
    { type$ "proceedings" =
        'editor.organization.sort
        { type$ "manual" =
            'author.organization.sort
            'author.sort
          if$
        }
      if$
    }
  if$
  "    "
  *
  year field.or.null sortify
  *
  "    "
  *
  title field.or.null
  sort.format.title
  *
  #1 entry.max$ substring$
  'sort.label :=
  sort.label *
  #1 entry.max$ substring$
  'sort.key$ :=
}

ITERATE {presort}

SORT

STRINGS { longest.label last.label next.extra }

INTEGERS { longest.label.width last.extra.num number.label }

FUNCTION {initialize.longest.label}
{ "" 'longest.label :=
  #0 int.to.chr$ 'last.label :=
  "" 'next.extra :=
  #0 'longest.label.width :=
  #0 'last.extra.num :=
  #0 'number.label :=
}

%% PWD
FUNCTION {forward.pass}
{ last.label label =
    { last.extra.num #1 + 'last.extra.num :=
      last.extra.num int.to.chr$ 'extra.label :=
    }
    { "a" chr.to.int$ 'last.extra.num :=
      "" 'extra.label :=
      label 'last.label :=
    }
  if$
  number.label #1 + 'number.label :=
}

FUNCTION {reverse.pass}
{ next.extra "b" =
    { "a" 'extra.label := }
    'skip$
  if$
  extra.label 'next.extra :=
  extra.label label swap$ *
  'label :=
%%  'extra.label :=
%%  label extra.label * 'label :=
}

EXECUTE {initialize.longest.label}

ITERATE {forward.pass}

REVERSE {reverse.pass}

FUNCTION {bib.sort.order}
{ sort.label 'sort.key$ :=
%%% vorher { sort.label "}" * 'sort.key$ :=
}

ITERATE {bib.sort.order}

SORT


FUNCTION {begin.bib}
{   preamble$ empty$
    'skip$
    { preamble$ write$ newline$ }
  if$
  "\begin{thebibliography}{" number.label int.to.str$ * "}" *
  write$ newline$
  "\providecommand{\url}[1]{\texttt{#1}}"
  write$ newline$
  "\expandafter\ifx\csname urlstyle\endcsname\relax"
  write$ newline$
  "  \providecommand{\doi}[1]{doi: #1}\else"
  write$ newline$
  "  \providecommand{\doi}{doi: \begingroup \urlstyle{rm}\Url}\fi"
  write$ newline$
}



EXECUTE {begin.bib}

EXECUTE {init.state.consts}

ITERATE {call.type$}

FUNCTION {end.bib}
{ newline$
  "\end{thebibliography}" write$ newline$
}

EXECUTE {end.bib}

%% Ende von DIN11Xc.BST  KFL, 6.11.04
