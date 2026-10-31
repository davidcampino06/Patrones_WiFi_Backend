package com.wifisense.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceIdentificationTest {

    @Test
    void theAliasTheUserWroteComesFirst() {
        assertThat(DeviceIdentification.of("Celular de Ana", "Galaxy-A54", NameSource.CONFIRMED, "Samsung", "PHONE"))
                .isEqualTo(DeviceIdentification.ALIAS);
    }

    @Test
    void anAutomaticNameSaysWhereItCameFrom() {
        assertThat(DeviceIdentification.of(null, "Galaxy-A54", NameSource.CONFIRMED, null, null))
                .isEqualTo(DeviceIdentification.CONFIRMED);
        assertThat(DeviceIdentification.of(" ", "Galaxy-A54", NameSource.ROUTER, null, null))
                .isEqualTo(DeviceIdentification.ROUTER);
        assertThat(DeviceIdentification.of(null, "PC-SALA", NameSource.ANNOUNCED, null, null))
                .isEqualTo(DeviceIdentification.ANNOUNCED);
        assertThat(DeviceIdentification.of(null, "pc.lan", NameSource.DNS, null, null))
                .isEqualTo(DeviceIdentification.DNS);
    }

    @Test
    void aNameFromAnOldAgentIsANameWithoutSource() {
        assertThat(DeviceIdentification.of(null, "PC-SALA", null, null, null)).isEqualTo(DeviceIdentification.NAMED);
    }

    @Test
    void theMakerOrKindAloneIsAnEstimateNotAName() {
        assertThat(DeviceIdentification.of(null, null, null, "Samsung", null))
                .isEqualTo(DeviceIdentification.ESTIMATED);
        assertThat(DeviceIdentification.of(null, "", null, null, "PHONE")).isEqualTo(DeviceIdentification.ESTIMATED);
    }

    @Test
    void nothingAtAllIsUnknown() {
        assertThat(DeviceIdentification.of(null, null, null, null, null)).isEqualTo(DeviceIdentification.UNKNOWN);
    }

    @Test
    void anUnknownSourceFromTheAgentIsIgnored() {
        assertThat(NameSource.parse("router")).isEqualTo(NameSource.ROUTER);
        assertThat(NameSource.parse(" confirmed ")).isEqualTo(NameSource.CONFIRMED);
        assertThat(NameSource.parse("NETBIOS")).isNull();
        assertThat(NameSource.parse(null)).isNull();
    }
}
