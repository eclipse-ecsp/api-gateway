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
            $("openapi-explorer").attr("spec-url", data[0].url);
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
        $("openapi-explorer").attr("spec-url", url);
    });
});