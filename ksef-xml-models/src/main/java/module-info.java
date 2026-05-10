/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

/**
 * JAXB-generated XML models for KSeF schemas. Owns the {@code xml.*}
 * package tree; ksef-client transitively re-exports the four root packages
 * so consumers see them via the SDK dependency.
 *
 * <p>UBL 2.1 + Polish PEPPOL types shared between PEF Invoice and PEF_KOR
 * CreditNote live under a single {@code xml.ubl.*} tree — eliminates the
 * 1288-class duplication that pre-consolidation existed between
 * {@code xml.pef.*} and {@code xml.pefkor.*}.
 */
module io.github.mgrtomaszzurawski.ksef.xml {

    requires transitive jakarta.xml.bind;

    // Four invoice/credit-note roots — exposed to consumers as JAXB
    // escape-hatch accessors on the typed Invoice / Document records
    // (ADR-030). xml.pef and xml.pefkor each contain just the root type
    // (InvoiceType / CreditNoteType) + ObjectFactory; the bulk of the
    // UBL type tree lives in xml.ubl.* below.
    exports io.github.mgrtomaszzurawski.ksef.xml.fa2;
    exports io.github.mgrtomaszzurawski.ksef.xml.fa3;
    exports io.github.mgrtomaszzurawski.ksef.xml.pef;
    exports io.github.mgrtomaszzurawski.ksef.xml.pefkor;

    // UPO and AUTH packages are SDK-internal but exported for ksef-client
    // mappers — qualified-export keeps them off the consumer surface.
    exports io.github.mgrtomaszzurawski.ksef.xml.upo to io.github.mgrtomaszzurawski.ksef;
    exports io.github.mgrtomaszzurawski.ksef.xml.auth to io.github.mgrtomaszzurawski.ksef;

    // UBL sub-packages used by ksef-client's internal mappers
    // (PefInvoice / PefKorInvoice / Document flat-accessor implementations).
    // Qualified export keeps them off the consumer surface; consumers see
    // only xml.fa2/fa3/pef/pefkor exported above (ADR-030).
    exports io.github.mgrtomaszzurawski.ksef.xml.ubl.cac to io.github.mgrtomaszzurawski.ksef;
    exports io.github.mgrtomaszzurawski.ksef.xml.ubl.cbc to io.github.mgrtomaszzurawski.ksef;

    // JAXB reflective access — all opens to jakarta.xml.bind. Module
    // count down from 30 (pre-consolidation) to 16 because PEF + PEF_KOR
    // sub-package duplicates are gone.
    opens io.github.mgrtomaszzurawski.ksef.xml.fa2 to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.fa3 to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.ubl.cac to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.ubl.cacpl to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.ubl.cbc to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.ubl.cbcpl to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.ubl.ccts to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.ubl.ext to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.ubl.sig to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.ubl.sigcac to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.ubl.sigcbc to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.ubl.udt to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.ubl.xades132 to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.ubl.xades141 to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.ubl.xmldsig to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.upo to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.auth to jakarta.xml.bind;
}
