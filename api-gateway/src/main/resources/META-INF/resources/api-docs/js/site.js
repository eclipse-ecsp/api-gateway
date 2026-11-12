$(document).ready(function () {
    if (localStorage.access_token != null) {
        localStorage.clear();
    }
    ;
    // The Advanced Search dialog lives in the explorer's shadow DOM and exposes no ::part hooks,
    // so its look is applied through an adopted stylesheet.
    const ADVANCED_SEARCH_CSS = `
        .dialog-box-overlay { background: rgba(15, 23, 42, .45); backdrop-filter: blur(3px); }
        .dialog-box { border: 0; border-radius: 12px; background: #fff; overflow: hidden;
                      box-shadow: 0 24px 60px rgba(15, 23, 42, .3); }
        .dialog-box-header { padding: 0 24px; min-height: 60px; border-bottom: 1px solid #eceff3; }
        .dialog-box-title { font-size: 16px; font-weight: 600; color: #1d2433; }
        .dialog-box-header button.m-btn { width: 30px; height: 30px; padding: 0; font-size: 18px; line-height: 1;
                                          color: #6b7385; background: transparent; border: 0; border-radius: 8px;
                                          cursor: pointer; }
        .dialog-box-header button.m-btn:hover { background: #eef0f4; color: #1d2433; }
        .dialog-box-content { padding: 20px 24px 24px; }
        #advanced-search-dialog-input { width: 100%; height: 40px; padding: 0 14px; font-size: 14px;
                                        color: #1d2433; background: #f7f8fa; border: 1px solid #e4e7ec;
                                        border-radius: 8px; outline: none;
                                        transition: border-color .15s, box-shadow .15s, background .15s; }
        #advanced-search-dialog-input:focus { background: #fff; border-color: var(--primary-color);
                                              box-shadow: 0 0 0 3px rgba(62, 96, 119, .2); }
        .advanced-search-locations { margin-top: 14px; display: grid; gap: 10px 20px;
                                     grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); }
        .advanced-search-locations label { font-size: 13px; color: #4a5265; cursor: pointer; }
    `;

    function styleAdvancedSearch() {
        const root = document.querySelector('openapi-explorer').shadowRoot;
        if (!root || !('adoptedStyleSheets' in root)) {
            return;
        }
        const sheet = new CSSStyleSheet();
        sheet.replaceSync(ADVANCED_SEARCH_CSS);
        root.adoptedStyleSheets = [...root.adoptedStyleSheets, sheet];
    }

    customElements.whenDefined('openapi-explorer').then(styleAdvancedSearch);

    function renderTokenState() {
        const saved = !!localStorage.access_token;
        $('#tokenBox').toggleClass('saved', saved);
        $("#jwtToken").attr('readonly', saved);
        $('#jwtTokenSave').prop('disabled', saved);
        $('#jwtTokenClear').prop('disabled', !saved && !$("#jwtToken").val());
        $('#jwtTokenSave').text(saved ? 'Saved' : 'Save');
    }

    $('#jwtTokenSave').click(function () {
        // Accept a pasted "Bearer <token>" value as well as a bare token
        const token = $("#jwtToken").val().trim().replace(/^bearer\s+/i, '');
        if (!token) {
            $("#jwtToken").focus();
            return;
        }
        localStorage.clear();
        localStorage.access_token = token;
        $("#jwtToken").val(token);
        renderTokenState();
    });
    $('#jwtTokenClear').click(function () {
        localStorage.clear();
        $("#jwtToken").val("").focus();
        renderTokenState();
    });
    $('#jwtToken').on('keydown', function (event) {
        if (event.key === 'Enter') {
            $('#jwtTokenSave').click();
        }
    }).on('input', renderTokenState);
    $('#jwtTokenToggle').click(function () {
        const reveal = $("#jwtToken").attr('type') === 'password';
        $("#jwtToken").attr('type', reveal ? 'text' : 'password');
        $(this).attr('aria-label', reveal ? 'Hide token' : 'Show token');
    });
    renderTokenState();
    $.get("/v3/api-docs/swagger-config", function (data, status) {
        console.log(data);
        $.each(data, function (index, group) {
            $('#api-group').append('<option value="' + group.url + '">' + group.name + '</option>');
        });
        if (data != null && data.length > 0) {
            // Default select first entry
           setApiSpec(data[0].url);
        }
    });
    $.ajaxSetup({
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Accept", "application/json");
            // Set the Authorization header with the access token
            if (localStorage.access_token) {
                 xhr.setRequestHeader("Authorization", "Bearer " + localStorage.access_token);
            }
        }
    });
    $("openapi-explorer").on('request', function (event) {
        if(localStorage.access_token) {
            event.detail.request.headers.append('Authorization', `Bearer ${localStorage.access_token}`);
        }

    });
    $("#api-group").change(function () {
        var url = $("#api-group option:selected").attr('value');
        console.log("selected URL - " + url);
        //$("openapi-explorer").attr("spec-url", url);
        setApiSpec(url);
    });

    // Utility functions for UI state management
    function showLoading() {
        $('#loading-indicator').show();
        $('#error-container').hide();
        $('openapi-explorer').hide();
    }

    function showExplorer() {
        $('#loading-indicator').hide();
        $('#error-container').hide();
        $('openapi-explorer').show();
    }

    function showError(url, errorMessage) {
        $('#loading-indicator').hide();
        $('openapi-explorer').hide();
        $('#error-url').text(url);
        $('#error-message').text(errorMessage);
        $('#error-container').show();
    }

    // Global retry function
    window.retryLoadSpec = function() {
        const url = $("#api-group option:selected").attr('value');
        if (url) {
            setApiSpec(url);
        } else {
            console.error('No API group selected');
        }
    }

    // Fetch with timeout support
    function fetchWithTimeout(url, options = {}, timeout = 30000) {
        return Promise.race([
            fetch(url, options),
            new Promise((_, reject) =>
                setTimeout(() => reject(new Error('Request timeout - server took too long to respond')), timeout)
            )
        ]);
    }

    setApiSpec = async function (specUrl) {
        console.log('Loading API specification from:', specUrl);
        showLoading();
        
        try {
            // Fetch the API specification with timeout
            // Get timeout from env if available, otherwise default to 15000ms
            const timeout = 15000;
            const response = await fetchWithTimeout(specUrl, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json',
                    'Authorization': localStorage.access_token ? `Bearer ${localStorage.access_token}` : ''
                }
            }, timeout);
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            
            const contentType = response.headers.get('content-type');
            if (!contentType || !contentType.includes('application/json')) {
                throw new Error(`Invalid content type: ${contentType}. Expected application/json`);
            }
            
            const data = await response.json();
            
            // Validate that we have a valid OpenAPI spec
            if (!data.openapi && !data.swagger) {
                throw new Error('Invalid API specification: missing openapi or swagger version');
            }
            
            const explorer = document.getElementsByTagName('openapi-explorer')[0];
            await explorer.loadSpec(data);
            showExplorer();
            console.log('API specification loaded successfully');
            
        } catch (error) {
            console.error('Failed to load API specification:', error);
            
            let errorMessage = error.message;
            
            // Provide user-friendly error messages
            if (error.message.includes('timeout')) {
                errorMessage = 'Request timeout - the server took too long to respond. Please try again.';
            } else if (error.message.includes('Failed to fetch')) {
                errorMessage = 'Network error - unable to connect to the server. Please check your connection.';
            } else if (error.message.includes('JSON')) {
                errorMessage = 'Invalid response format - the server returned malformed data.';
            }
            
            showError(specUrl, errorMessage);
        }
    }
});