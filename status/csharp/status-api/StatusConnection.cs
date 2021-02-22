using System;
using System.ComponentModel;
using System.Threading.Tasks;
using RestSharp;

namespace org_comroid_status_api
{
    public sealed class StatusConnection
    {
        public static readonly string BaseUrl = "https://api.comroid.org/";
        public static StatusConnection Instance;
        private readonly string _serviceName;
        internal readonly string Token;
        internal readonly RestClient _rest;

        public Service OwnService { get; private set; }

        public StatusConnection(string serviceName, string token = null)
        {
            Instance = this;

            _serviceName = serviceName;
            Token = token;
            _rest = new RestClient(BaseUrl);

            Task<Service> req = RequestServiceByName(serviceName);
            req.GetAwaiter().OnCompleted(() => OwnService = req.Result);
        }

        public async Task<Service> RequestServiceByName(string serviceName)
        {
            RestRequest req = new RestRequest($"service/{serviceName}", Method.GET, DataFormat.Json);
            Service yield = await Task.Run(() => _rest.Execute<Service>(req).Data);
            return yield;
        }
    }
}