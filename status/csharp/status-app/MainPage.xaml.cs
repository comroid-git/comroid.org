using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Timers;
using Windows.System;
using Windows.UI.Core;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using org_comroid_status_api;
using Windows.ApplicationModel.Core;

// Die Elementvorlage "Leere Seite" wird unter https://go.microsoft.com/fwlink/?LinkId=402352&clcid=0x407 dokumentiert.

namespace status_app
{
    /// <summary>
    ///     Eine leere Seite, die eigenständig verwendet oder zu der innerhalb eines Rahmens navigiert werden kann.
    /// </summary>
    public sealed partial class MainPage : Page
    {
        public static readonly Uri Homepage = new Uri("https://status.comroid.org");
        internal static readonly StatusConnection Connection = new StatusConnection();

        public MainPage()
        {
            InitializeComponent();
            new Timer(5 * 1000) { AutoReset = true, Enabled = true }.Elapsed += (sender, args) => ReloadPage(sender, null);
        }

        private async void ReloadPage(object sender, RoutedEventArgs e)
        {
            await CoreApplication.MainView.CoreWindow.Dispatcher.RunAsync(CoreDispatcherPriority.Normal, async () =>
            {
                Debug.WriteLine("Initiating Page reload");
                ReloadingIndicator.Visibility = Visibility.Visible;

                List<Service> services = await Connection.RefreshServiceCache();

                foreach (Service service in services)
                {
                    ServiceBox existing = ComputeServiceBox(service);
                    existing.UpdateDisplay(service);
                }

                ReloadingIndicator.Visibility = Visibility.Collapsed;
                Debug.WriteLine(
                    $"Reload complete with {services.Count} services; Stacker has {ServiceList.Children.Count} children");
            });
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

        private async void OpenInBrowser(object sender, RoutedEventArgs e)
        {
            await Launcher.LaunchUriAsync(Homepage);
        }
    }
}