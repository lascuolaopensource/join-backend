jQuery(document).ready(function ($) {

    function getDifferentRandom(prev, max) {
        var i;
        do {
            i = Math.floor(Math.random() * max);
        } while (i === prev);
        return i;
    }

    function logoLoop() {
        var FONT_FEATS = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'];
        var N_LETTERS = 'SOS'.length;
        var oldRandoms = [0, 0, 0];
        window.setInterval(function () {
            for (var i = 0; i < N_LETTERS; i++) {
                var r = getDifferentRandom(oldRandoms[i], FONT_FEATS.length);
                $('.navbar-brand > span:nth-of-type(' + (i+1) + ')').attr('class', 'font-feat-' + FONT_FEATS[r]);
                oldRandoms[i] = r;
            }
        }, 300);

        var mainNavbar = $('#main-navbar');
        mainNavbar.on('show.bs.collapse', function () {
            $('body').css('overflow', 'hidden');
        });
        mainNavbar.on('hidden.bs.collapse', function () {
            $('body').css('overflow', 'auto');
        });
    }


    function formValidation() {
        var forms = $('.needs-validation');
        forms.each(function (i, form) {
            $(form).on('submit', function (event) {
                if (form.checkValidity() === false) {
                    event.preventDefault();
                    event.stopPropagation();
                }
                form.classList.add('was-validated');
            });
        });
    }


    logoLoop();
    formValidation();

});
