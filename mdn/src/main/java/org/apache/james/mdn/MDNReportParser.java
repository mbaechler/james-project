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
        Rule cfws() {
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
            return Ch((char)0x20);
        }

        //         HTAB           =  %x09
        Rule htab() {
            return Ch((char)0x09);
        }

        //         CRLF           =  CR LF
        Rule crlf() {
            return Sequence(cr(), lf());
        }

        //         CR             =  %x0D
        Rule cr() {
            return Ch((char)0x0D);
        }

        //         LF             =  %x0A
        Rule lf() {
            return Ch((char)0x0A);
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
                                 obs-ctext   */
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
                                 %d127              ;  white space characters   */
        Rule obsNoWsCtl() {
            return FirstOf(
                CharRange((char)1, (char)8),
                Ch((char)11),
                Ch((char)12),
                CharRange((char)14, (char)31),
                Ch((char)127));
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
                    Ch((char)0xd0),
                    obsCtext(),
                    lf(),
                    cr()));
        }

        //    mdn-request-header = "Disposition-Notification-To" ":" mailbox-list CRLF
        Rule mdnRequestHeader() {
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
                                 "~"   */
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
                                 [CFWS]   */
        Rule quotedString() {
            return Sequence(
                cfws(),
                Sequence(dquote(), ZeroOrMore(Sequence(fws(), qcontent()), Optional(fws()), dquote())),
                cfws());
        }

        //         DQUOTE         =  %x22
        Rule dquote() {
            return Ch((char)0x22);
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
                                 obs-angle-addr   */
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
                                 *("," [CFWS] ["@" domain])   */
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
                                 obs-dtext          ;  "[", "]", or "\"   */
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

        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        /*    disposition-notification-content =
                     [ reporting-ua-field CRLF ]
                     [ mdn-gateway-field CRLF ]
                     [ original-recipient-field CRLF ]
                     final-recipient-field CRLF
                     [ original-message-id-field CRLF ]
                     disposition-field CRLF
                     *( error-field CRLF )
                     *( extension-field CRLF )    */
        Rule dispositionNotificationContent() {
            return Sequence(
                Optional(Sequence(reportingUaField(), crlf())),
                Optional(Sequence(mdnGatewayField(), crlf())),
                Optional(Sequence(originalRecipientField(), crlf())),
                Sequence(finalRecipientField(), crlf()),
                Optional(Sequence(originalMessageId(), crlf())),
                Sequence(dispositionField(), crlf()),
                ZeroOrMore(Sequence(errorField(), crlf())),
                ZeroOrMore(Sequence(extentionField(), crlf())));
        }

        /*    reporting-ua-field = "Reporting-UA" ":" OWS ua-name OWS [
                                   ";" OWS ua-product OWS ]    */
        Rule reportingUaField() {
            return Sequence("Reporting UA", ":", ows(), uaName(), ows(),
                Optional(Sequence(";", ows(), uaProduct(), ows())));
        }

        //    ua-name = *text-no-semi
        Rule uaName() {
            return ZeroOrMore(textNoSemi());
        }

        /*    text-no-semi = %d1-9 /        ; "text" characters excluding NUL, CR,
                             %d11 / %d12 / %d14-58 / %d60-127      ; LF, or semi-colon    */
        Rule textNoSemi() {
            return FirstOf(
                CharRange((char)1, (char)9),
                Character.toChars(11),
                Character.toChars(12),
                CharRange((char)14, (char)58),
                CharRange((char)60, (char)127));
        }

        //    ua-product = *([FWS] text)
        Rule uaProduct() {
            return ZeroOrMore(Sequence(Optional(fws()), text()));
        }

        /*   text            =   %d1-9 /            ; Characters excluding CR
                                 %d11 /             ;  and LF
                                 %d12 /
                                 %d14-127   */
        Rule text() {
            return FirstOf(
                    CharRange((char)1, (char)9),
                    Character.toChars(11),
                    Character.toChars(12),
                    CharRange((char)14, (char)127));
        }

        /*    OWS = [CFWS]
                    ; Optional whitespace.
                    ; MDN generators SHOULD use "*WSP"
                    ; (Typically a single space or nothing.
                    ; It SHOULD be nothing at the end of a field.),
                    ; unless an RFC 5322 "comment" is required.
                    ;
                    ; MDN parsers MUST parse it as "[CFWS]".    */
        Rule ows() {
            return Optional(cfws());
        }

        /*    mdn-gateway-field = "MDN-Gateway" ":" OWS mta-name-type OWS
                                  ";" OWS mta-name    */
        Rule mdnGatewayField() {
            return Sequence("MDN-Gateway", ":", mtaNameType(), ows(), ";", ows(), mtaName());
        }

        //    mta-name-type = Atom
        Rule mtaNameType() {
            return atom();
        }

        //    mta-name = *text
        Rule mtaName() {
            return ZeroOrMore(text());
        }

        /*    original-recipient-field =
                     "Original-Recipient" ":" OWS address-type OWS
                     ";" OWS generic-address OWS    */
        Rule originalRecipientField() {
            return Sequence("Original-Recipient", ":", ows(), addressType(), ows(),
                ";", ows(), genericAddress(), ows());
        }

        //    address-type = Atom
        Rule addressType() {
            return atom();
        }

        //    generic-address = *text
        Rule genericAddress() {
            return ZeroOrMore(text());
        }

        /*    final-recipient-field =
                     "Final-Recipient" ":" OWS address-type OWS
                     ";" OWS generic-address OWS    */
        Rule finalRecipientField() {
            return Sequence("Final-Recipient", ":", ows(), addressType(), ows(),
                ";", ows(), genericAddress(), ows());
        }

        //    original-message-id-field = "Original-Message-ID" ":" msg-id
        Rule originalMessageId() {
            return Sequence("Original-Message-ID", msgId());
        }

        //    msg-id          =   [CFWS] "<" id-left "@" id-right ">" [CFWS]
        Rule msgId() {
            return Sequence(Optional(cfws()), "<", idLeft(), "@", idRight(), ">", Optional(cfws()));
        }

        //   id-left         =   dot-atom-text / obs-id-left
        Rule idLeft() {
            return FirstOf(dotAtomText(), obsIdLeft());
        }

        //   obs-id-left     =   local-part
        Rule obsIdLeft() {
            return localPart();
        }

        //   obs-id-right    =   domain
        Rule idRight() {
            return domain();
        }

        /*    disposition-field =
                     "Disposition" ":" OWS disposition-mode OWS ";"
                     OWS disposition-type
                     [ OWS "/" OWS disposition-modifier
                     *( OWS "," OWS disposition-modifier ) ] OWS    */
        Rule dispositionField() {
            return Sequence(
                "Disposition", ":", ows(), dispositionMode(), ows(), ";",
                ows(), dispositionType(),
                Optional(Sequence(ows(), "/", ows(), dispositionModifier(),
                    ZeroOrMore(Sequence(ows(), ",", dispositionModifier())))), ows());
        }

        //    disposition-mode = action-mode OWS "/" OWS sending-mode
        Rule dispositionMode() {
            return Sequence(actionMode(), ows(), "/", ows(), sendingMode());
        }

        //    action-mode = "manual-action" / "automatic-action"
        Rule actionMode() {
            return FirstOf("manual-action", "automatic-action");
        }

        //    sending-mode = "MDN-sent-manually" / "MDN-sent-automatically"
        Rule sendingMode() {
            return FirstOf("MDN-sent-manually", "MDN-sent-automatically");
        }

        /*    disposition-type = "displayed" / "deleted" / "dispatched" /
                      "processed"    */
        Rule dispositionType() {
            return FirstOf("displayed", "deleted", "dispatched", "processed");
        }

        //    disposition-modifier = "error" / disposition-modifier-extension
        Rule dispositionModifier() {
            return FirstOf("error", dispositionModifierExtension());
        }

        //    disposition-modifier-extension = Atom
        Rule dispositionModifierExtension() {
            return atom();
        }

        //    error-field = "Error" ":" *([FWS] text)
        Rule errorField() {
            return Sequence("Error", ":", ZeroOrMore(Sequence(Optional(fws()), text())));
        }

        //    extension-field = extension-field-name ":" *([FWS] text)
        Rule extentionField() {
            return Sequence(extensionFieldName(), ":", ZeroOrMore(Sequence(Optional(fws()), text())));
        }

        //    extension-field-name = field-name
        Rule extensionFieldName() {
            return fieldName();
        }

        //   field-name      =   1*ftext
        Rule fieldName() {
            return OneOrMore(ftext());
        }

        /*   ftext           =   %d33-57 /          ; Printable US-ASCII
                                 %d59-126           ;  characters not including
                                                    ;  ":".   */
        Rule ftext() {
            return FirstOf(
                    CharRange((char)33, (char)57),
                    CharRange((char)59, (char)126));
        }
    }
}
