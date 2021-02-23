using System;
using System.Collections.Generic;
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
        internal readonly RestClient Rest;
        public readonly Dictionary<String, Entity> Cache;

        public Service OwnService { get; private set; }

        public StatusConnection(string serviceName, string token = null)
        {
            Instance = this;

            _serviceName = serviceName;
            Token = token;
            Rest = new RestClient(BaseUrl);
            Cache = new Dictionary<string, Entity>();

            Task<Service> req = RequestServiceByName(serviceName);
            req.GetAwaiter().OnCompleted(() => OwnService = req.Result);
        }

        public async Task<Service> RequestServiceByName(string serviceName)
        {
            RestRequest req = new RestRequest($"service/{serviceName}", Method.GET, DataFormat.Json);
            Service yield = await Task.Run(() => Rest.Execute<Service>(req).Data);
            return yield;
        }
    }
}