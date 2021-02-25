using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using RestSharp;

namespace org_comroid_status_api
{
    public sealed class StatusConnection
    {
        public static readonly string BaseUrl = "https://api.status.comroid.org/";
        public static StatusConnection Instance;
        private readonly string _serviceName;
        internal readonly string Token;
        internal readonly RestClient Rest;
        public readonly Dictionary<String, Entity> Cache;

        public Service OwnService { get; private set; }

        public StatusConnection() : this(null)
        {
        }

        public StatusConnection(string serviceName, string token = null)
        {
            Instance = this;

            _serviceName = serviceName;
            Token = token;
            Rest = new RestClient(BaseUrl);
            Cache = new Dictionary<string, Entity>();

            if (serviceName != null)
            {
                Task<Service> req = RequestServiceByName(serviceName);
                req.GetAwaiter().OnCompleted(() => OwnService = req.Result);
            }
        }

        public IEnumerable<Service> GetServices()
        {
            return Cache.Values.Cast<Service>();
        }

        public async Task<Service> RequestServiceByName(string serviceName)
        {
            RestRequest req = new RestRequest($"service/{serviceName}", Method.GET, DataFormat.Json);
            IRestResponse<Service> response = await Task.Run(() => Rest.Execute<Service>(req));
            if (response.Data != null)
                throw new IOException($"Could not request service {serviceName}", response.ErrorException);
            return response.Data;
        }

        public async Task<List<Service>> RequestServices()
        {
            RestRequest req = new RestRequest("services", Method.GET, DataFormat.Json);
            IRestResponse<List<Service>> response = await Task.Run(() => Rest.Execute<List<Service>>(req));
            if (response.Data == null)
                throw new IOException("Could not request services", response.ErrorException);
            return response.Data;
        }

        public async Task<List<Service>> RefreshServiceCache()
        {
            List<Service> yields = await RequestServices();
            for (var i = 0; i < yields.Count; i++)
            {
                Service srv = yields[i];
                var sname = srv.Name;

                if (!Cache.ContainsKey(sname))
                    Cache.Add(sname, srv);
                else
                {
                    Service cached = Cache[sname] as Service;
                    cached.CopyFrom(srv);
                    yields[i] = cached;
                }
            }
            return yields;
        }
    }
}