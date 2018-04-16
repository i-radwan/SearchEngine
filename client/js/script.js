//
// Constants
//
const SERVER_SEARCH_LINK = "http://localhost:8080/search?q={query}&page={page}";
const SERVER_SUGGESTIONS_LINK = "http://localhost:8080/suggestions?q={query}";
const MIN_SUGGESTION_CHARS_COUNT = 3;

//
// Main App Code
//
let app = {
    run: function () {
        app.searchBox = $("#search_box");
        app.suggestions = [];
        app.didTheUserSearch = false;

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

    /**
     * Server request
     *
     * @param query
     * @param page
     */
    getWebpagesRequest: function (query, page = 1) {
        if (app.didTheUserSearch) // Check to prevent the effect the first time only
            $("#results_container").fadeTo("fast", 0.3); // Fade out to give refresh animation

        $.ajax({
            url: SERVER_SEARCH_LINK.replace("{query}", query).replace("{page}", page),
            type: 'GET',
            dataType: 'jsonp'
        });

        app.didTheUserSearch = true;
    },

    /**
     * Server request
     *
     * @param query
     * @param page
     */
    getSuggestionsRequest: function (query) {
        $.ajax({
            url: SERVER_SUGGESTIONS_LINK.replace("{query}", query),
            type: 'GET',
            dataType: 'jsonp'
        });
    },

    /**
     * JSON callback which is passed in the URL to handle webpages
     *
     * @param response
     */
    webpagesCallBack: function (response) {
        if (response.hasOwnProperty("error_msg")) {
            return alert(response["error_msg"]);
        }

        // Slide search bar to top
        $(".main").animate({'marginTop': '10vh'}, 700);

        // Fill containers
        app.displayResults(response.pages);
        app.displayPagination(response.pagination);
    },

    /**
     * JSON callback which is passed in the URL to handle suggestions
     *
     * @param response
     */
    suggestionsCallBack: function (response) {
        app.suggestions = response;
        app.updateSearchBoxAutoCompleteList();
    },

    /**
     * Render webpages into results container
     *
     * @param webpages
     */
    displayResults: function (webpages) {
        app.styleSnippet(webpages, app.searchBox.val().split(" "));
        app.resultsContainer.html(app.resultsTemplateScript({webpages: webpages}));

        $("#results_container").fadeTo("fast", 1);
    },

    /**
     * Dispaly pagination
     *
     * @param pagination
     */
    displayPagination: function (pagination) {
        // Check if current page in the first/last segment
        let isFirstSegment = (pagination.current_page <= 6);
        let isLastSegment = (pagination.current_page >= pagination.pages_count - 5);

        // Push first page (1)
        let pagesNumbers = [{
            number: 1,
            active: true,
            current: pagination.current_page === 1
        }];

        // Push (...) if pages are in need to be hidden
        if (!isFirstSegment) pagesNumbers.push({active: false, current: false});

        // Get segment before current page
        for (let i = Math.max(2, pagination.current_page - 4); i <= pagination.current_page; i++)
            pagesNumbers.push({
                number: i,
                active: true,
                current: i === pagination.current_page
            });

        // Get segment after current page
        for (let i = pagination.current_page + 1; i <= Math.min(pagination.pages_count - 1, pagination.current_page + 4); i++)
            pagesNumbers.push({
                number: i,
                active: true,
                current: i === pagination.current_page
            });

        // Push (...) if pages are in need to be hidden
        if (!isLastSegment) pagesNumbers.push({active: false, current: false});

        // Push last page
        if (pagination.pages_count > pagination.current_page)
            pagesNumbers.push({
                number: pagination.pages_count,
                active: true,
                current: pagination.current_page === pagination.pages_count
            });

        // Fill the container
        app.paginationContainer.html(app.paginationTemplateScript({page_numbers: pagesNumbers}));

        // Reset link handlers
        app.setPaginationLinksHandler();

        // Back to top
        $("html, body").animate({scrollTop: 0}, "fast");
    },

    /**
     * Make keywords bold in the snipper
     *
     * ToDo: check if will get it ready from the server
     *
     * @param webpages
     * @param keywords
     */
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

    /**
     * Catch pressing enter to search
     */
    configureSearchBox: function () {
        // Send search request when enter key gets pressed
        app.searchBox.bind('keypress', function (e) {
            if (e.keyCode === 13 && app.searchBox.val().trim().length > 0) {
                app.getWebpagesRequest(app.searchBox.val());
            }
        });

        // Send suggestions retrieval request if the content exceeds limit
        app.searchBox.bind('keyup', function (e) {
            if (app.searchBox.val().length > MIN_SUGGESTION_CHARS_COUNT) {
                app.getSuggestionsRequest(app.searchBox.val());
            }
        });
    },

    config: function () {
        // Bind event listener
        app.configureSearchBox();
    },

    /**
     * Prevent default links action, re-fetch new page from server
     */
    setPaginationLinksHandler: function () {
        $(".pagination .valid").click(function (event) {
            event.preventDefault();
            app.getWebpagesRequest(app.searchBox.val(), $(this).data("page"));
        });
    },

    /**
     * Update the autocomplete list after fetching from server
     */
    updateSearchBoxAutoCompleteList: function () {
        // Set autocomplete list
        app.searchBox.autocomplete({
            source: app.suggestions
        });
    }
};

//
// Run
//
$(document).ready(function () {
    app.run();
});