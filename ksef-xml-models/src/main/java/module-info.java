/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

/**
 * JAXB-generated XML models for KSeF schemas. Owns the {@code xml.*}
 * package tree; ksef-client transitively re-exports the four root packages
 * so consumers see them via the SDK dependency.
 */
module io.github.mgrtomaszzurawski.ksef.xml {

    requires transitive jakarta.xml.bind;

    // Four invoice/credit-note roots — exposed to consumers as JAXB escape-hatch
    // accessors on the typed Invoice / Document records (ADR-030).
    exports io.github.mgrtomaszzurawski.ksef.xml.fa2;
    exports io.github.mgrtomaszzurawski.ksef.xml.fa3;
    exports io.github.mgrtomaszzurawski.ksef.xml.pef;
    exports io.github.mgrtomaszzurawski.ksef.xml.pefkor;

    // UPO and AUTH packages are SDK-internal but exported for ksef-client
    // mappers — qualified-export keeps them off the consumer surface.
    exports io.github.mgrtomaszzurawski.ksef.xml.upo to io.github.mgrtomaszzurawski.ksef;
    exports io.github.mgrtomaszzurawski.ksef.xml.auth to io.github.mgrtomaszzurawski.ksef;

    // UBL sub-packages used by ksef-client's internal mappers
    // (PefInvoice/PefKorInvoice flat-accessor implementations). Qualified
    // export keeps them off the consumer surface; consumers see only the
    // four root xml.fa2/fa3/pef/pefkor packages exported above (ADR-030).
    exports io.github.mgrtomaszzurawski.ksef.xml.pef.cac to io.github.mgrtomaszzurawski.ksef;
    exports io.github.mgrtomaszzurawski.ksef.xml.pef.cbc to io.github.mgrtomaszzurawski.ksef;
    exports io.github.mgrtomaszzurawski.ksef.xml.pefkor.cac to io.github.mgrtomaszzurawski.ksef;
    exports io.github.mgrtomaszzurawski.ksef.xml.pefkor.cbc to io.github.mgrtomaszzurawski.ksef;

    // JAXB reflective access. The four exported roots plus the 24 UBL
    // sub-packages need to be opened so the JAXB context builder can
    // resolve qualified element names. opens-to-module is reflection-only
    // and does not contribute to the public API surface.
    opens io.github.mgrtomaszzurawski.ksef.xml.fa2 to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.fa3 to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.cac to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.cacpl to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.cbc to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.cbcpl to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.ccts to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.ext to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.sig to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.sigcac to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.sigcbc to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.udt to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.xades132 to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.xades141 to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.xmldsig to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.cac to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.cacpl to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.cbc to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.cbcpl to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.ccts to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.ext to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.sig to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.sigcac to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.sigcbc to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.udt to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.xades132 to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.xades141 to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.xmldsig to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.upo to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.auth to jakarta.xml.bind;
}
