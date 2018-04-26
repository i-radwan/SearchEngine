//
// Constants
//
const SERVER_SEARCH_LINK = "http://localhost:8080/search?q={query}&page={page}";
const SERVER_SUGGESTIONS_LINK = "http://localhost:8080/suggestions?q={query}";
const MIN_SUGGESTION_CHARS_COUNT = 3;
const SUGGESTIONS_TIMEOUT_DELAY = 300;
const RESULTS_PER_PAGE = 12; // Max snippets per search result
const QUERY_MAX_LENGTH = 5 * 10;

//
// Main App Code
//
let app = {
    run: function () {
        app.searchBox = $("#search_box");
        app.suggestions = [];
        app.suggestionsTimeouts = [];
        app.didTheUserSearch = false;
        app.enterHitTime = 0;

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
     * Retrieve webpages from the server
     *
     * @param query
     * @param page
     */
    getWebpagesRequest: function (query, page = 1) {
        app.requestSent = true;

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
     * Retrieve suggestions from the server
     */
    getSuggestionsRequest: function () {
        let query = app.searchBox.val();

        if (query[0] == "\"")
            query = query.substr(1, query.length - 2);

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
        $(".main").animate({'marginTop': '10vh'}, {
            duration: 700,
            complete: function () {
                app.requestSent = false;
            }
        });

        // Fill containers
        app.displayResults(response.pages);
        app.displayPaginationAndInfo(response.pagination);
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
     * Renders webpages into results container
     *
     * @param webpages
     */
    displayResults: function (webpages) {
        app.resultsContainer.html(app.resultsTemplateScript({webpages: webpages}));

        $("#results_container").fadeTo("fast", 1);
    },

    /**
     * Displays pagination
     *
     * @param pagination
     */
    displayPaginationAndInfo: function (pagination) {
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
        app.paginationContainer.html(app.paginationTemplateScript({
            page_numbers: pagesNumbers,
        }));

        // Reset link handlers
        app.setPaginationLinksHandler();

        // Back to top
        $("html, body").animate({scrollTop: 0}, "fast");

        // Fill info

        $("#results-time").html((Date.now() - app.enterHitTime) / 1000.0 + " s");
        $("#results-count").html(pagination.pages_count * RESULTS_PER_PAGE);
        $(".info").css('display', 'block');
    },

    /**
     * Catches pressing enter to search
     */
    configureSearchBox: function () {
        // Send search request when enter key gets pressed
        app.searchBox.bind('keypress', function (e) {
            if (e.keyCode === 13 && app.searchBox.val().trim().length > 0) {
                app.enterHitTime = Date.now();
                app.getWebpagesRequest(app.searchBox.val());
            }
        });

        $(".search-btn").click(function () {
            if (app.searchBox.val().trim().length > 0) {
                app.enterHitTime = Date.now();
                app.getWebpagesRequest(app.searchBox.val());
            }
        });

        // Send suggestions retrieval request if the content exceeds limit
        app.searchBox.bind('keyup', function (e) {
            if (app.searchBox.val().length > MIN_SUGGESTION_CHARS_COUNT) {
                app.clearAllTimeOuts();
                app.suggestionsTimeouts.push(setTimeout(app.getSuggestionsRequest, SUGGESTIONS_TIMEOUT_DELAY));
            }
        });

        // Set max length
        app.searchBox.attr('maxlength', QUERY_MAX_LENGTH);
    },
    clearAllTimeOuts() {
        for (let i = 0; i < app.suggestionsTimeouts.length; i++) {
            clearTimeout(app.suggestionsTimeouts[i]);
        }

        app.suggestionsTimeouts = [];
    },

    config: function () {
        // Bind event listener
        app.configureSearchBox();
    },

    /**
     * Prevents default links action, re-fetch new page from server
     */
    setPaginationLinksHandler: function () {
        $(".pagination .valid").click(function (event) {
            event.preventDefault();
            app.getWebpagesRequest(app.searchBox.val(), $(this).data("page"));
        });
    },

    /**
     * Updates the autocomplete list after fetching from server
     */
    updateSearchBoxAutoCompleteList: function () {
        // Set autocomplete list
        app.searchBox.autocomplete({
            source: function (request, response) {
                if (app.requestSent) return;

                let tmpSuggestions = Object.assign([], app.suggestions);
                if (request.term[0] == "\"") {
                    for (let i = 0; i < tmpSuggestions.length; ++i) {
                        tmpSuggestions[i] = "\"" + app.suggestions[i] + "\"";
                    }
                }
                console.log($.ui.autocomplete);
                let matcher = new RegExp($.ui.autocomplete.escapeRegex(request.term), "i");
                response($.grep(tmpSuggestions, function (value) {
                    value = value.label || value.value || value;
                    return matcher.test(value);
                }));
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