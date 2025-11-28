$(document).ready(function () {
    if (localStorage.access_token != null) {
        localStorage.clear();
    }
    ;
    $('#jwtTokenSave').click(function () {
        localStorage.clear();
        //localStorage.role = role;
        localStorage.access_token = $("#jwtToken").val();
        console.log("access token: " + localStorage.access_token);
        $("#jwtToken").css("background-color", "gray");
        $("#jwtToken").attr('readonly', true);
    });
    $('#jwtTokenClear').click(function () {
        localStorage.clear();
        $("#jwtToken").val("");
        $("#jwtToken").css("background-color", "white");
        $("#jwtToken").attr('readonly', false);
    });
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
            const response = await fetchWithTimeout(specUrl, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json',
                    'Authorization': localStorage.access_token ? `Bearer ${localStorage.access_token}` : ''
                }
            }, 10000); // 10 second timeout
            
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