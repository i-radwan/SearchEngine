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
        let linkTemp = SERVER_LINK.replace("{query}", app.searchBox.val());

        // Get range around current page
        let isFirstSegment = (pagination.current_page <= 6);
        let isLastSegment = (pagination.current_page >= pagination.pages_count - 5);

        let pagesNumbers = [{
            number: 1,
            active: true,
            current: pagination.current_page === 1,
            link: linkTemp.replace("{page}", 1)
        }];

        if (!isFirstSegment) pagesNumbers.push({active: false, current: false});

        for (let i = Math.max(2, pagination.current_page - 4); i <= pagination.current_page; i++)
            pagesNumbers.push({
                number: i,
                active: true,
                current: i === pagination.current_page,
                link: linkTemp.replace("{page}", i)
            });


        for (let i = pagination.current_page + 1; i <= Math.min(pagination.pages_count - 1, pagination.current_page + 4); i++)
            pagesNumbers.push({
                number: i,
                active: true,
                current: i === pagination.current_page,
                link: linkTemp.replace("{page}", i)
            });

        if (!isLastSegment) pagesNumbers.push({active: false, current: false});

        if (pagination.pages_count > pagination.current_page)
            pagesNumbers.push({
                number: pagination.pages_count,
                active: true,
                current: pagination.current_page === pagination.pages_count,
                link: linkTemp.replace("{page}", pagination.pages_count)
            });

        app.paginationContainer.html(app.paginationTemplateScript({page_numbers: pagesNumbers}));

        app.setPaginationLinksHandler();

        // Back to top
        $("html, body").animate({scrollTop: 0}, "fast");
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
    },
    setPaginationLinksHandler: function () {
        $(".pagination .valid").click(function (event) {
            event.preventDefault();
            app.getWebpagesRequest(app.searchBox.val(), $(this).data("page"));
        });
    }
};

//
// Run
//
$(document).ready(function () {
    app.run();
});