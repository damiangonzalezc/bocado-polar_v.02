package cl.bocadopolar.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import cl.bocadopolar.auth.exception.SolicitudInvalidaException;

class ValidacionesTest {

    @Test
    void correoValidoTerminaEnClOCom() {
        assertThat(Validaciones.correo(" Cliente@Bocado.CL ")).isEqualTo("cliente@bocado.cl");
        assertThat(Validaciones.correo("ana.perez@gmail.com")).isEqualTo("ana.perez@gmail.com");
    }

    @Test
    void correoConDobleTerminacionSeRechaza() {
        assertThatThrownBy(() -> Validaciones.correo("ana@empresa.com.cl"))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> Validaciones.correo("ana@empresa.org"))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void rutConDigitoVerificadorCorrecto() {
        assertThat(Validaciones.rut("12.345.678-5")).isEqualTo("12345678-5");
        assertThat(Validaciones.rut("")).isNull();
    }

    @Test
    void rutConDigitoIncorrectoSeRechaza() {
        assertThatThrownBy(() -> Validaciones.rut("12.345.678-9"))
                .isInstanceOf(SolicitudInvalidaException.class);
    }
}
