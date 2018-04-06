//
// Constants
//
const SERVER_LINK = "http://0.0.0.0:8080/search?q={query}&page={page}";


//
// Main App Code
//
let app = {
    run: function () {
        app.searchBox = $("#search_box");

        app.config();

        //
        // Results container and template
        //
        app.resultsContainer = $('#results_container');

        app.resultsTemplate = $('#results-temp').html();
        app.resultsTemplateScript = Handlebars.compile(app.resultsTemplate);


        //
        // Pagination container and template
        //
        app.paginationContainer = $('#pagination_container');

        app.paginationTemplate = $('#pagination-temp').html();
        app.paginationTemplateScript = Handlebars.compile(app.paginationTemplate);
    },
    search: function () {
        let searchQuery = app.searchBox.val();

        app.getWebpagesRequest(searchQuery);
    },
    getWebpagesRequest: function (query, page = 1) {
        $.ajax({
            url: SERVER_LINK.replace("{query}", query).replace("{page}", page),
            type: 'GET',
            dataType: 'jsonp'
        });
    },
    webpagesCallBack: function (response) {
        app.displayResults(response.pages);
        app.displayPagination(response.pagination);
    },
    displayResults: function (webpages) {
        app.styleSnippet(webpages, app.searchBox.val().split(" "));
        app.resultsContainer.html(app.resultsTemplateScript({webpages: webpages}));
    },
    displayPagination: function (pagination) {
        app.paginationContainer.html(app.paginationTemplateScript({pagination: pagination}));
    },
    // ToDo: check if will get it ready from the server
    styleSnippet: function (webpages, keywords) {
        for (let i in webpages) {
            let page = webpages[i];

            for (let j in keywords) {
                let keyword = keywords[j];
                if (keyword.length === 0) continue;

                page.snippet = page.snippet.replace(new RegExp("\\b" + keyword + "\\b", 'g'), "<b>" + keyword + "</b>");
            }
        }
    },
    bindEventListeners: function () {
        // Call search function of enter key press
        app.searchBox.bind('keypress', function (e) {
            if (e.keyCode === 13) {
                app.search();
            }
        });
    },
    config: function () {
        // Bind event listener
        app.bindEventListeners();

        //
        // Configure handlebars
        //

        // Support for loop
        // Handlebars.registerHelper('for', function (n, block) {
        //     let accum = '';
        //     for (let i = 0; i < n; ++i)
        //         accum += block.fn(i);
        //     return accum;
        // });
    }
};

//
// Run
//
$(document).ready(function () {
    app.run();
});