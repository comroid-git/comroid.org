using System;
using System.ComponentModel;
using System.Threading.Tasks;
using RestSharp;

namespace org_comroid_status_api
{
    public sealed class StatusConnection
    {
        public static readonly string BaseUrl = "https://api.comroid.org/";
        private readonly string _serviceName;
        private readonly string _token;
        private readonly RestClient _rest;

        public Service OwnService { get; private set; }

        public StatusConnection(string serviceName, string token = null)
        {
            _serviceName = serviceName;
            _token = token;
            _rest = new RestClient(BaseUrl);

            Task<Service> req = RequestServiceByName(serviceName);
            req.GetAwaiter().OnCompleted(() => OwnService = req.Result);
        }

        public async Task<Service> RequestServiceByName(string serviceName)
        {
            RestRequest req = new RestRequest($"service/{serviceName}", Method.GET, DataFormat.Json);

            return _rest.Execute<Service>(req).Data;
        }
    }
}