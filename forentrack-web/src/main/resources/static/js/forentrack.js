/* ForenTrack · comportamiento de la interfaz (sin dependencias aparte de Bootstrap) */
(function () {
    'use strict';

    // 1) Confirmación antes de enviar formularios marcados con data-confirmar="mensaje"
    const modalConfirmar = document.getElementById('ftConfirmar');
    let formularioPendiente = null;
    document.addEventListener('submit', function (evento) {
        const form = evento.target;
        if (form.dataset.confirmar && !form.dataset.confirmado && modalConfirmar && window.bootstrap) {
            evento.preventDefault();
            formularioPendiente = form;
            modalConfirmar.querySelector('[data-confirmar-texto]').textContent = form.dataset.confirmar;
            const boton = modalConfirmar.querySelector('[data-confirmar-ok]');
            boton.className = 'btn ' + (form.dataset.confirmarTono || 'btn-primary');
            bootstrap.Modal.getOrCreateInstance(modalConfirmar).show();
            return;
        }
        // 2) Validación visual en el navegador (el servidor vuelve a validar)
        if (form.classList.contains('needs-validation') && !form.checkValidity()) {
            evento.preventDefault();
            evento.stopPropagation();
            form.classList.add('was-validated');
            const primero = form.querySelector(':invalid');
            if (primero) { primero.focus(); }
            return;
        }
        // 3) Evita el doble envío y muestra que se está procesando
        form.querySelectorAll('button[type="submit"]').forEach(function (b) {
            b.disabled = true;
            if (!b.querySelector('.spinner-border')) {
                b.insertAdjacentHTML('afterbegin', '<span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>');
            }
        });
    }, true);

    if (modalConfirmar) {
        modalConfirmar.querySelector('[data-confirmar-ok]').addEventListener('click', function () {
            if (formularioPendiente) {
                formularioPendiente.dataset.confirmado = 'si';
                bootstrap.Modal.getInstance(modalConfirmar).hide();
                formularioPendiente.requestSubmit ? formularioPendiente.requestSubmit() : formularioPendiente.submit();
            }
        });
    }

    // 4) Modales compartidos: el botón que los abre indica la URL de la acción (data-url) y una referencia
    document.addEventListener('show.bs.modal', function (evento) {
        const disparador = evento.relatedTarget;
        const modal = evento.target;
        if (!disparador) { return; }
        const form = modal.querySelector('form');
        if (form && disparador.dataset.url) {
            form.setAttribute('action', disparador.dataset.url);
            form.classList.remove('was-validated');
            form.reset();
        }
        modal.querySelectorAll('[data-ref]').forEach(function (el) {
            el.textContent = disparador.dataset.ref || '';
        });
    });

    // 5) Las alertas de éxito se cierran solas
    document.querySelectorAll('[data-autocerrar]').forEach(function (alerta) {
        setTimeout(function () {
            if (window.bootstrap) { bootstrap.Alert.getOrCreateInstance(alerta).close(); }
        }, 7000);
    });

    // 6) Búsqueda rápida en tablas: <input data-filtro-tabla="#idTabla">
    document.querySelectorAll('[data-filtro-tabla]').forEach(function (entrada) {
        const tabla = document.querySelector(entrada.dataset.filtroTabla);
        if (!tabla) { return; }
        entrada.addEventListener('input', function () {
            const texto = entrada.value.trim().toLowerCase();
            tabla.querySelectorAll('tbody tr').forEach(function (fila) {
                fila.hidden = texto !== '' && !fila.textContent.toLowerCase().includes(texto);
            });
        });
    });

    // 7) Fechas que no pueden ser futuras
    document.querySelectorAll('input[data-max-ahora]').forEach(function (entrada) {
        const ahora = new Date();
        ahora.setMinutes(ahora.getMinutes() - ahora.getTimezoneOffset());
        entrada.max = ahora.toISOString().slice(0, 16);
    });

    // 8) Mostrar u ocultar contraseña
    document.querySelectorAll('[data-ver-clave]').forEach(function (boton) {
        boton.addEventListener('click', function () {
            const campo = document.querySelector(boton.dataset.verClave);
            const visible = campo.type === 'text';
            campo.type = visible ? 'password' : 'text';
            boton.querySelector('.bi').className = 'bi ' + (visible ? 'bi-eye' : 'bi-eye-slash');
        });
    });

    // 9) Formulario de usuario: muestra las acciones exclusivas del rol elegido
    const selectorRol = document.querySelector('[data-rol-selector]');
    if (selectorRol) {
        const actualizar = function () {
            document.querySelectorAll('[data-rol-info]').forEach(function (bloque) {
                bloque.hidden = bloque.dataset.rolInfo !== selectorRol.value;
            });
            const vacio = document.querySelector('[data-rol-vacio]');
            if (vacio) { vacio.hidden = selectorRol.value !== ''; }
        };
        selectorRol.addEventListener('change', actualizar);
        actualizar();
    }

    // 10) Activa los tooltips de Bootstrap
    if (window.bootstrap) {
        document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(function (el) {
            bootstrap.Tooltip.getOrCreateInstance(el);
        });
    }
})();
