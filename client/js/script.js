//
// Constants
//
const SERVER_LINK = "http://0.0.0.0:8080/search?q={query}";


//
// Main App Code
//
let app = {
    run: function () {
        app.searchBox = $("#search_box");

        app.bindEventListeners();

        app.resultsContainer = $('#results_container');

        app.resultsTemplate = $('#results-temp').html();
        app.resultsTemplateScript = Handlebars.compile(app.resultsTemplate);
    },
    search: function () {
        let searchQuery = app.searchBox.val();

        app.getWebpagesRequest(searchQuery);
    },
    getWebpagesRequest: function (query) {
        $.ajax({
            url: SERVER_LINK.replace("{query}", query),
            type: 'GET',
            dataType: 'jsonp'
        });
    },
    webpagesCallBack: function (webpages) {
        let context = {"pages": webpages};

        app.resultsContainer.html(app.resultsTemplateScript(context));
    },
    bindEventListeners: function () {
        // Call search function of enter key press
        app.searchBox.bind('keypress', function (e) {
            if (e.keyCode === 13) {
                app.search();
            }
        });
    }
};

//
// Run
//
$(document).ready(function () {
    app.run();
});