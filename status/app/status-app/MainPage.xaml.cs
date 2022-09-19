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
        private static StatusServerBox _rootBox;

        public bool AutoUpdate { get; internal set; }

        public MainPage()
        {
            DataContext = this;
            InitializeComponent();
            new Timer(5 * 1000) {AutoReset = true, Enabled = true}.Elapsed +=
                async (sender, args) =>
                {
                    await CoreApplication.MainView.CoreWindow.Dispatcher.RunAsync(CoreDispatcherPriority.Normal,
                        async () =>
                        {
                            if (AutoUpdate)
                                ReloadPage(sender, null);
                        });
                };
            AutoUpdate = true;
        }

        private async void ReloadPage(object sender, RoutedEventArgs e)
        {
            Debug.WriteLine("Initiating Page reload");
            ReloadingIndicator.Visibility = Visibility.Visible;

            List<Service> services = null;
            try
            {
                services = await Connection.RefreshServiceCache();
            }
            catch (Exception ex)
            {
                UIElementCollection children = ((Panel) _rootBox.Parent).Children;
                foreach (UIElement uiElement in children.Where(it => it != _rootBox).ToArray())
                    children.Remove(uiElement);
                _rootBox.ServiceName = "Status Server is Unreachable";
                _rootBox.StatusText = $"Could not fetch services - [{ex.GetType().Name}]: {ex.Message}";
                _rootBox.StatusColor = ServiceBox.ConvertColor(ServiceStatus.OfflineColor);
                services = new List<Service>();
            }

            foreach (Service service in services)
            {
                ServiceBox existing = ComputeServiceBox(service);
                existing.UpdateDisplay(service);
            }

            ReloadingIndicator.Visibility = Visibility.Collapsed;
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

        private async void OpenInBrowser(object sender, RoutedEventArgs e)
        {
            await Launcher.LaunchUriAsync(Homepage);
        }

        private void RootBoxLoaded(object sender, RoutedEventArgs e)
        {
            _rootBox = sender as StatusServerBox;
        }

        private void UpdateDoAutoReload(object sender, RoutedEventArgs e)
        {
            AutoUpdate = DoAutoReload.IsChecked ?? false;
        }
    }
}