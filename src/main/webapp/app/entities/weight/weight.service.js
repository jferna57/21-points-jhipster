(function() {
    'use strict';
    angular
        .module('21PointsApp')
        .factory('Weight', Weight);

    Weight.$inject = ['$resource', 'DateUtils'];

    function Weight ($resource, DateUtils) {
        var resourceUrl =  'api/weights/:id';

        return $resource(resourceUrl, {}, {
            'query': { method: 'GET', isArray: true},
            'last30Days' : { method: 'GET', isArray: false, url: 'api/weights-by-days/30'},
            'get': {
                method: 'GET',
                transformResponse: function (data) {
                    if (data) {
                        data = angular.fromJson(data);
                        data.dateTime = DateUtils.convertDateTimeFromServer(data.dateTime);
                    }
                    return data;
                }
            },
            'update': { method:'PUT' }
        });
    }
})();
