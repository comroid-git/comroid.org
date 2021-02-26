using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Threading;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.System;
using Windows.UI;
using Windows.UI.Xaml.Media;
using org_comroid_status_api;

// Die Elementvorlage "Leere Seite" wird unter https://go.microsoft.com/fwlink/?LinkId=402352&clcid=0x407 dokumentiert.

namespace status_app
{
    /// <summary>
    /// Eine leere Seite, die eigenständig verwendet oder zu der innerhalb eines Rahmens navigiert werden kann.
    /// </summary>
    public sealed partial class MainPage : Page
    {
        public static readonly Uri Homepage = new Uri("https://status.comroid.org");
        internal static readonly StatusConnection Connection = new StatusConnection();

        public MainPage()
        {
            this.InitializeComponent();
        }

        private async void ReloadPage(object sender, RoutedEventArgs e)
        {
            Debug.WriteLine("Initiating Page reload");

            List<Service> services = await Connection.RefreshServiceCache();

            foreach (Service service in services)
            {
                ServiceBox existing = ComputeServiceBox(service);
                existing.UpdateDisplay(service);
            }

            Debug.WriteLine(
                $"Reload complete with {services.Count} services; Stacker has {ServiceList.Children.Count} children");
        }

        private ServiceBox ComputeServiceBox(Service service)
        {
            ServiceBox @default = ServiceList.Children
                .Select(each => each as ServiceBox)
                .Where(each => each != null)
                .FirstOrDefault(box => box.Name.Equals($"status-{service.Name}"));
            if (@default != null)
                return @default;
            ServiceBox inst = new ServiceBox(service.Name);
            ServiceList.Children.Add(inst);
            return inst;
        }

        private void InitializeServiceList(object sender, RoutedEventArgs e)
        {
            ReloadPage(sender, e);
        }

        private async void OpenInBrowser(object sender, RoutedEventArgs e)
        {
            await Launcher.LaunchUriAsync(Homepage);
        }
    }
}