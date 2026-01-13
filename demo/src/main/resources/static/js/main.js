/*

    SHOWPAGE FREE TEMPLATE BY IAMSUPVIEW.BE

    01. Sticky Navbar
    02. Modal (opcional)
    03. Smooth Scrolling
    04. MediaCheck
    05. Animations (make-it-appear)
    06. Preloader

*/

var ajax_form = true;

$(document).ready(function () {

/* --------------------------------------------------------------------------
    01. NAVBAR STICKY + SELECTED
--------------------------------------------------------------------------- */

    var cbpAnimatedHeader = (function () {

        var docElem = document.documentElement,
            header = document.querySelector('.cbp-af-header'),
            didScroll = false,
            changeHeaderOn = 100;

        function init() {
            if (!header) return; // <-- FIX
            window.addEventListener('scroll', function () {
                if (!didScroll) {
                    didScroll = true;
                    setTimeout(scrollPage, 0);
                }
            });
        }

        function scrollPage() {
            var sy = window.pageYOffset || docElem.scrollTop;
            if (sy >= changeHeaderOn) {
                header.classList.add('cbp-af-header-shrink');
            } else {
                header.classList.remove('cbp-af-header-shrink');
            }
            didScroll = false;
        }

        init();

    })();


/* --------------------------------------------------------------------------
    02. MODAL (solo si existe en la página)
--------------------------------------------------------------------------- */

    var modal        = document.getElementById('modal'),
        modalContent = document.querySelector('.modal-content'),
        openModalBtns = document.querySelectorAll('.js-open-modal'),
        closeModal   = document.querySelectorAll('.modal, .modal-close');

    // Solo activar si el modal existe
    if (modal && modalContent && openModalBtns.length > 0) {

        // TODOS los botones con clase js-open-modal abren el mismo modal
        openModalBtns.forEach(function (btn) {
            btn.addEventListener('click', function (e) {
                e.preventDefault();
                modal.classList.add('is-visible');
            });
        });

        // Cerrar haciendo click en el fondo oscuro o en la X
        closeModal.forEach(function (el) {
            el.addEventListener('click', function () {
                modal.classList.remove('is-visible');
            });
        });

        // Evitar que el click dentro del contenido cierre el modal
        modalContent.addEventListener('click', function (event) {
            event.stopPropagation();
        });

        // (Opcional) que aparezca automático después de unos ms
        // setTimeout(function () {
        //     modal.style.display = 'block';
        // }, 250);
    }



/* --------------------------------------------------------------------------
    03. SMOOTH SCROLLING
--------------------------------------------------------------------------- */

    $('.goto').click(function (e) {
        e.preventDefault();
        $('html').scrollTo(this.hash, this.hash);
    });


/* --------------------------------------------------------------------------
    04. MEDIA CHECK
--------------------------------------------------------------------------- */

    mediaCheck({
        media: '(max-width: 768px)',
        entry: function () {

            $('.make-it-appear-top').waypoint(function () {
                $(this).css('opacity', '1');
            }, { offset: '200%' });

            $('.make-it-appear-left').waypoint(function () {
                $(this).css('opacity', '1');
            }, { offset: '200%' });

            $('.make-it-appear-right').waypoint(function () {
                $(this).css('opacity', '1');
            }, { offset: '200%' });

            $('.make-it-appear-bottom').waypoint(function () {
                $(this).css('opacity', '1');
            }, { offset: '200%' });
        },

        exit: function () {

            $('.make-it-appear-top').waypoint(function () {
                $(this).addClass('animated fadeInDown');
            }, { offset: '80%' });

            $('.make-it-appear-left').waypoint(function () {
                $(this).addClass('animated fadeInLeft');
            }, { offset: '80%' });

            $('.make-it-appear-right').waypoint(function () {
                $(this).addClass('animated fadeInRight');
            }, { offset: '80%' });

            $('.make-it-appear-bottom').waypoint(function () {
                $(this).addClass('animated fadeInUp');
            }, { offset: '80%' });

            $('.bounce').waypoint(function () {
                $(this).addClass('animated bounce');
            }, { offset: '70%' });

            $('.pulse').waypoint(function () {
                $(this).addClass('animated pulse');
            }, { offset: '50%' });
        }
    });

}); // END document ready



/* --------------------------------------------------------------------------
    06. PRELOADER
--------------------------------------------------------------------------- */

$(window).on('load', function () {
    $("#loading-animation").fadeOut();
    $("#preloader").delay(600).fadeOut("slow");
});
