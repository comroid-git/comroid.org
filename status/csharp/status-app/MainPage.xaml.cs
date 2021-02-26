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
            return ServiceList.Children
                       .Select(each => each as ServiceBox)
                       .Where(each => each != null)
                       .FirstOrDefault(box => box.Name.Equals($"status_{service.Name.Replace('-', '_')}"))
                   ?? new ServiceBox(this, service);
        }

        private void InitializeServiceList(object sender, RoutedEventArgs e)
        {
            ReloadPage(sender, e);
        }

        private async void OpenInBrowser(object sender, RoutedEventArgs e)
        {
            await Launcher.LaunchUriAsync(Homepage);
        }

        internal sealed class ServiceBox : Frame
        {
            internal ServiceBox(MainPage mainPage, Service service)
            {
                Name = $"status_{service.Name.Replace('-', '_')}";
                Template = mainPage.Resources["ServiceBoxTemplate"] as ControlTemplate
                           ?? throw new MissingMemberException("ServiceBoxTemplate not found");

                mainPage.ServiceList.Children.Add(this);
            }

            public void UpdateDisplay(Service service)
            {
                if (!Name.Equals($"status_{service.Name.Replace('-', '_')}"))
                    throw new ArgumentException("Service ID mismatch");
                ServiceStatus status = service.GetStatus();
                /*
                TextBlock _statusText = (Content as StackPanel).Children[1] as TextBlock;
                _statusText.Text = status.Display;
                _statusText.Foreground = new SolidColorBrush(Color.FromArgb(status.DisplayColor.A,
                    status.DisplayColor.R, status.DisplayColor.G, status.DisplayColor.B));
                */
            }
        }
    }
}