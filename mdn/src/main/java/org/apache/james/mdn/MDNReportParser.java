package org.apache.james.mdn;

import java.util.Optional;

import org.parboiled.BaseParser;
import org.parboiled.Rule;

import com.google.common.annotations.VisibleForTesting;

public class MDNReportParser {
    private static final boolean STRICT = true;
    private static final boolean LENIENT = false;
    public static final boolean DEFAULT_STRICT = true;

    private final Optional<Boolean> strict;

    public MDNReportParser() {
        this(Optional.empty());
    }

    MDNReportParser(Optional<Boolean> strict) {
        this.strict = strict;
    }

    public MDNReportParser strict() {
        return new MDNReportParser(Optional.of(STRICT));
    }

    public MDNReportParser lenient() {
        return new MDNReportParser(Optional.of(LENIENT));
    }

    public Optional<MDNReport> parse(String mdnReport) {
        if (this.strict.orElse(DEFAULT_STRICT)) {
            throw new IllegalStateException();
        }
        return Optional.empty();
    }

    @VisibleForTesting
    static class Parser extends BaseParser<MDNReport> {
        //   CFWS            =   (1*([FWS] comment) [FWS]) / FWS
        public Rule cfws() {
            return FirstOf(
                Sequence(
                    OneOrMore(Sequence(fws(), comment())),
                    fws()),
                fws());
        }

        //   FWS             =   ([*WSP CRLF] 1*WSP) /  obs-FWS
        Rule fws() {
            return FirstOf(
                Sequence(
                    Optional(Sequence(
                        ZeroOrMore(wsp()),
                        crlf())),
                    OneOrMore(wsp())),
                obsFWS());
        }

        //         WSP            =  SP / HTAB
        Rule wsp() {
            return FirstOf(sp(), htab());
        }

        //         SP             =  %x20
        Rule sp() {
            return AnyOf(Character.toChars(0x20));
        }

        //         HTAB           =  %x09
        Rule htab() {
            return AnyOf(Character.toChars(0x09));
        }

        //         CRLF           =  CR LF
        Rule crlf() {
            return Sequence(cr(), lf());
        }

        //         CR             =  %x0D
        private Object cr() {
            return AnyOf(Character.toChars(0x0D));
        }

        //         LF             =  %x0A
        Rule lf() {
            return AnyOf(Character.toChars(0x0A));
        }

        //   obs-FWS         =   1*WSP *(CRLF 1*WSP)
        Rule obsFWS() {
            return Sequence(
                OneOrMore(wsp()),
                ZeroOrMore(Sequence(
                    crlf(),
                    OneOrMore(wsp()))));
        }

        //   comment         =   "(" *([FWS] ccontent) [FWS] ")"
        Rule comment() {
            return Sequence(
                "(",
                ZeroOrMore(Sequence(
                    fws(),
                    ccontent()
                    )),
                Optional(fws()),
                ")");
        }

        //   ccontent        =   ctext / quoted-pair / comment
        Rule ccontent() {
            return FirstOf(ctext(), quotedPair(), comment());
        }

        /*   ctext           =   %d33-39 /          ; Printable US-ASCII
                                 %d42-91 /          ;  characters not including
                                 %d93-126 /         ;  "(", ")", or "\"
                                 obs-ctext*/
        Rule ctext() {
            return FirstOf(
                CharRange((char)33, (char)39),
                CharRange((char)42, (char)91),
                CharRange((char)93, (char)126),
                obsCtext());
        }

        //   obs-ctext       =   obs-NO-WS-CTL
        Rule obsCtext() {
            return obsNoWsCtl();
        }

        /*   obs-NO-WS-CTL   =   %d1-8 /            ; US-ASCII control
                                 %d11 /             ;  characters that do not
                                 %d12 /             ;  include the carriage
                                 %d14-31 /          ;  return, line feed, and
                                 %d127              ;  white space characters*/
        Rule obsNoWsCtl() {
            return FirstOf(
                CharRange((char)1, (char)8),
                AnyOf(Character.toChars(11)),
                AnyOf(Character.toChars(12)),
                CharRange((char)14, (char)31),
                AnyOf(Character.toChars(127)));
        }

        //   quoted-pair     =   ("\" (VCHAR / WSP)) / obs-qp
        Rule quotedPair() {
            return FirstOf(
                Sequence(
                    "\\",
                    FirstOf(vchar(), wsp())),
                obsQp());
        }

        //         VCHAR          =  %x21-7E
        Rule vchar() {
            return CharRange((char)0x21, (char)0x7E);
        }

        //   obs-qp          =   "\" (%d0 / obs-NO-WS-CTL / LF / CR)
        Rule obsQp() {
            return Sequence(
                "\\",
                FirstOf(
                    AnyOf(Character.toChars(0xd0)),
                    obsCtext(),
                    lf(),
                    cr()));
        }

        //    mdn-request-header = "Disposition-Notification-To" ":" mailbox-list CRLF
        public Rule mdnRequestHeader() {
            return Sequence("Disposition-Notification-To", ":", mailboxList(), crlf());
        }

        //   mailbox-list    =   (mailbox *("," mailbox)) / obs-mbox-list
        Rule mailboxList() {
            return FirstOf(
                Sequence(
                    mailbox(),
                    ZeroOrMore(Sequence(",", mailbox()))),
                obsMboxList());
        }

        //   mailbox         =   name-addr / addr-spec
        Rule mailbox() {
            return FirstOf(nameAddr(), addrSpec());
        }

        //   name-addr       =   [display-name] angle-addr
        Rule nameAddr() {
            return Sequence(Optional(displayName()), angleAddr());
        }

        Rule displayName() {
            return phrase();
        }

        //   phrase          =   1*word / obs-phrase
        Rule phrase() {
            return FirstOf(OneOrMore(word()), obsPhrase());
        }

        //   word            =   atom / quoted-string
        Rule word() {
            return FirstOf(atom(), quotedString());
        }

        //    atom            =   [CFWS] 1*atext [CFWS]
        Rule atom() {
            return Sequence(
                Optional(cfws()),
                OneOrMore(atext()),
                Optional(cfws()));
        }

        /*   atext           =   ALPHA / DIGIT /    ; Printable US-ASCII
                                 "!" / "#" /        ;  characters not including
                                 "$" / "%" /        ;  specials.  Used for atoms.
                                 "&" / "'" /
                                 "*" / "+" /
                                 "-" / "/" /
                                 "=" / "?" /
                                 "^" / "_" /
                                 "`" / "{" /
                                 "|" / "}" /
                                 "~"*/
        Rule atext() {
            return FirstOf(
                alpha(), digit(),
                "!", "#",
                "$", "%",
                "&", "'",
                "*", "+",
                "-", "/",
                "=", "?",
                "^", "_",
                "`", "{",
                "|", "}",
                "~");
        }

        //         ALPHA          =  %x41-5A / %x61-7A   ; A-Z / a-z
        Rule alpha() {
            return FirstOf(CharRange((char)0x41, (char)0x5A), CharRange((char)0x61, (char)0x7A));
        }

        //         DIGIT          =  %x30-39
        Rule digit() {
            return CharRange((char)0x30, (char)0x39);
        }

        /*   quoted-string   =   [CFWS]
                                 DQUOTE *([FWS] qcontent) [FWS] DQUOTE
                                 [CFWS]*/
        Rule quotedString() {
            return Sequence(
                cfws(),
                Sequence(dquote(), ZeroOrMore(Sequence(fws(), qcontent()), Optional(fws()), dquote())),
                cfws());
        }

        //         DQUOTE         =  %x22
        Rule dquote() {
            return AnyOf(Character.toChars(0x22));
        }

        //   qcontent        =   qtext / quoted-pair
        Rule qcontent() {
            return FirstOf(qcontent(), quotedPair());
        }

        //   obs-phrase      =   word *(word / "." / CFWS)
        Rule obsPhrase() {
            return Sequence(word(), ZeroOrMore(Sequence(word(), ".", cfws())));
        }

        /*   angle-addr      =   [CFWS] "<" addr-spec ">" [CFWS] /
                                 obs-angle-addr*/
        Rule angleAddr() {
            return FirstOf(
                Sequence(Optional(cfws()), "<", addrSpec(), ">", Optional(cfws())),
                obsAngleAddr());
        }

        //   obs-angle-addr  =   [CFWS] "<" obs-route addr-spec ">" [CFWS]
        Rule obsAngleAddr() {
            return Sequence(Optional(cfws()), "<", obsRoute(), addrSpec(), Optional(cfws()));
        }

        //   obs-route       =   obs-domain-list ":"
        Rule obsRoute() {
            return Sequence(obsDomainList(), ":");
        }

        /*   obs-domain-list =   *(CFWS / ",") "@" domain
                                 *("," [CFWS] ["@" domain])*/
        Rule obsDomainList() {
            return Sequence(
                ZeroOrMore(FirstOf(cfws(), ",")), "@", domain(),
                ZeroOrMore(Sequence(",", Optional(cfws()), Optional(Sequence("@", domain())))));
        }

        //   domain          =   dot-atom / domain-literal / obs-domain
        Rule domain() {
            return FirstOf(dotAtom(), domainLiteral(), obsDomain());
        }

        //   dot-atom        =   [CFWS] dot-atom-text [CFWS]
        Rule dotAtom() {
            return Sequence(Optional(cfws()), dotAtomText(), Optional(cfws()));
        }

        //   dot-atom-text   =   1*atext *("." 1*atext)
        Rule dotAtomText() {
            return Sequence(OneOrMore(atext()), ZeroOrMore(Sequence(".", OneOrMore(atext()))));
        }

        //   domain-literal  =   [CFWS] "[" *([FWS] dtext) [FWS] "]" [CFWS]
        Rule domainLiteral() {
            return Sequence(Optional(cfws()), "[", ZeroOrMore(Sequence(Optional(fws()), dtext()), Optional(fws()), "]", Optional(cfws())));
        }

        /*   dtext           =   %d33-90 /          ; Printable US-ASCII
                                 %d94-126 /         ;  characters not including
                                 obs-dtext          ;  "[", "]", or "\"*/
        Rule dtext() {
            return FirstOf(
                CharRange((char)33, (char)90),
                CharRange((char)94, (char)126),
                obsDtext());
        }

        //   obs-dtext       =   obs-NO-WS-CTL / quoted-pair
        Rule obsDtext() {
            return FirstOf(obsNoWsCtl(), quotedPair());
        }

        //   obs-domain      =   atom *("." atom)
        Rule obsDomain() {
            return Sequence(atom(), ZeroOrMore(Sequence(".", atom())));
        }

        //   addr-spec       =   local-part "@" domain
        Rule addrSpec() {
            return Sequence(localPart(), "@", domain());
        }

        //   local-part      =   dot-atom / quoted-string / obs-local-part
        Rule localPart() {
            return FirstOf(dotAtom(), quotedString(), obsLocalPart());
        }

        //   obs-local-part  =   word *("." word)
        Rule obsLocalPart() {
            return Sequence(word(), ZeroOrMore(Sequence(".", word())));
        }

        //   obs-mbox-list   =   *([CFWS] ",") mailbox *("," [mailbox / CFWS])
        Rule obsMboxList() {
            return Sequence(ZeroOrMore(Sequence(Optional(cfws()), ",")), mailbox(), ZeroOrMore(Sequence(",", Optional(FirstOf(mailbox(), cfws())))));
        }

    }
}
