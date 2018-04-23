//
// Constants
//
const SERVER_SEARCH_LINK = "http://localhost:8080/search?q={query}&page={page}";
const SERVER_SUGGESTIONS_LINK = "http://localhost:8080/suggestions?q={query}";
const MIN_SUGGESTION_CHARS_COUNT = 3;
const SUGGESTIONS_TIMEOUT_DELAY = 300;
const MAX_SNIPPETS_COUNT = 10; // Max snippets per search result

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
     * Renders webpages into results container
     *
     * @param webpages
     */
    displayResults: function (webpages) {
        app.extractSnippets(webpages, app.searchBox.val().split(" "));
        app.resultsContainer.html(app.resultsTemplateScript({webpages: webpages}));

        $("#results_container").fadeTo("fast", 1);
    },

    /**
     * Displays pagination
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
        app.paginationContainer.html(app.paginationTemplateScript({
            page_numbers: pagesNumbers,
            elapsed: Date.now() - app.enterHitTime
        }));

        // Reset link handlers
        app.setPaginationLinksHandler();

        // Back to top
        $("html, body").animate({scrollTop: 0}, "fast");
    },

    /**
     * Extracts
     *
     * @param webpages
     * @param queryWords
     */
    extractSnippets: function (webpages, queryWords) {
        for (let i in webpages) {
            let page = webpages[i];
            let pageContent = page.content;
            let pageContentArray = pageContent.split(" ");
            let pageContentArrayLength = pageContentArray.length;

            let nominatedSnippets = [];
            let lastKeywordIdx = -3;

            for (let key = 0; key < pageContentArrayLength; ++key) {
                let word = pageContentArray[key];
                if (queryWords.findIndex(item => app.removeSpecialCharsAroundWord(word.toLowerCase()) === app.removeSpecialCharsAroundWord(item.toLowerCase())) === -1) continue;

                let lastSnippet = nominatedSnippets[nominatedSnippets.length - 1];
                let snippet = {str: ""};
                let snippetStringStartIdx;

                // New snippet
                if (key - lastKeywordIdx > 2) {
                    snippet.L = Math.max(key - 2, (lastSnippet ? lastSnippet.R + 1 : 0));
                    snippet.R = Math.min(pageContentArrayLength, key + 2);

                    snippetStringStartIdx = snippet.L;

                    nominatedSnippets.push(snippet);
                }
                // Merge snippets
                else {
                    let oldLastSnippetR = lastSnippet.R;
                    lastSnippet.R = Math.min(pageContentArrayLength, key + 2);

                    snippet = lastSnippet;
                    snippetStringStartIdx = oldLastSnippetR + 1;
                }

                // Separate newly added words from the previous snippet.str words
                if (snippet === lastSnippet)
                    snippet.str += " ";

                // Add/Update snippet words
                for (let i = snippetStringStartIdx; i <= snippet.R; ++i) {
                    let isKeyword = (queryWords.findIndex(item => app.removeSpecialCharsAroundWord(pageContentArray[i].toLowerCase()) === app.removeSpecialCharsAroundWord(item.toLowerCase())) > -1);

                    snippet.str += (isKeyword) ? "<b>" : "";
                    snippet.str += pageContentArray[i];
                    snippet.str += (isKeyword) ? "</b>" : "";
                    snippet.str += (i < snippet.R) ? " " : "";
                }

                lastKeywordIdx = key;
            }

            // Sort by snippet length desc. (this will be better, and manages phrases too)
            nominatedSnippets.sort(function (a, b) {
                return (a.R - a.L) < (b.R - b.L);
            });

            let selectedSnippets = nominatedSnippets.splice(0, Math.min(10, nominatedSnippets.length));

            // Sort again by L to print them in order
            selectedSnippets.sort(function (a, b) {
                return (a.L) > (b.L);
            });

            // Concatenate to get page snippet
            page.snippet = "...";
            for (let i = 0; i < selectedSnippets.length; ++i) {
                page.snippet += selectedSnippets[i].str;
                page.snippet += (i < selectedSnippets.length - 1) ? "..." : "";
            }

            // Escaping route
            if (selectedSnippets.length === 0) {
                page.snippet = page.content.substr(0, 220) + "...";
            } else if (page.snippet.length < 220) {
                // Fill more to show full-like snippet
                page.snippet += page.content.substr(
                    selectedSnippets[selectedSnippets.length - 1].R + 1,
                    Math.min(page.content.length, 220 - page.snippet.length + 1)
                );
            }
        }
    },

    /**
     * Removes the special chars around word (google. => google)
     * This function will allow us to highlight more relevant things
     * @param word
     * @returns string processed word
     */
    removeSpecialCharsAroundWord: function (word) {
        let firstLetterIdx, lastLetterIdx;

        // Prefix chars
        for (let i = 0; i < word.length; ++i) {
            if (word[i].match("^[a-zA-Z0-9]*$") != null) {
                firstLetterIdx = i;
                break;
            }
        }

        // Postfix chars
        for (let i = word.length - 1; i >= 0; --i) {
            if (word[i].match("^[a-zA-Z0-9]*$") != null) {
                lastLetterIdx = i;
                break;
            }
        }

        return word.substr(firstLetterIdx, lastLetterIdx - firstLetterIdx + 1);
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

        // Send suggestions retrieval request if the content exceeds limit
        app.searchBox.bind('keyup', function (e) {
            if (app.searchBox.val().length > MIN_SUGGESTION_CHARS_COUNT) {
                app.clearAllTimeOuts();
                app.suggestionsTimeouts.push(setTimeout(app.getSuggestionsRequest, SUGGESTIONS_TIMEOUT_DELAY));
            }
        });
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