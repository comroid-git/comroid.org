using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.System;
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
        public T FindControl<T>(Type targetType, string ControlName) where T : FrameworkElement
        {
            return FindControl<T>(this, targetType, ControlName);
        }

        public T FindControl<T>(UIElement parent, Type targetType, string ControlName) where T : FrameworkElement
        {
            if (parent == null) return null;

            if (parent.GetType() == targetType && ((T) parent).Name == ControlName)
            {
                return (T) parent;
            }

            T result = null;
            int count = VisualTreeHelper.GetChildrenCount(parent);
            for (int i = 0; i < count; i++)
            {
                UIElement child = (UIElement) VisualTreeHelper.GetChild(parent, i);

                if (FindControl<T>(child, targetType, ControlName) != null)
                {
                    result = FindControl<T>(child, targetType, ControlName);
                    break;
                }
            }

            return result;
        }

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
                $"Reload complete with {services.Count} services; Stacker has {Stacker.Children.Count} children");
        }

        internal StackPanel Stacker => FindControl<StackPanel>(typeof(StackPanel), "ServicePanel");

        private ServiceBox ComputeServiceBox(Service service)
        {
            return Stacker.Children
                       .Cast<ServiceBox>()
                       .FirstOrDefault(box => box.Name.Equals($"status-{service.Name}"))
                   ?? new ServiceBox(Stacker, service);
        }

        private async void InitializeServiceList(object sender, RoutedEventArgs e)
        {
            ReloadPage(sender, e);
        }

        private async void OpenInBrowser(object sender, RoutedEventArgs e)
        {
            await Launcher.LaunchUriAsync(Homepage);
        }
    }

    internal sealed class ServiceBox : Panel
    {
        private readonly TextBox _displayName;
        private readonly TextBox _statusText;

        internal ServiceBox(StackPanel stacker, Service service)
        {
            Name = $"status-{service.Name}";
            this._displayName = new TextBox()
            {
                Text = service.DisplayName, Style = Resources["HeaderTextBlockStyle"] as Style,
                HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center
            };
            this._statusText = new TextBox()
            {
                Text = ServiceStatus.Unknown.Display, Style = Resources["HeaderTextBlockStyle"] as Style,
                HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center
            };
            Children.Add(_displayName);
            Children.Add(_statusText);
            stacker.Children.Add(this);

            UpdateDisplay(service);
        }

        public void UpdateDisplay(Service service)
        {
            if (!Name.Equals($"status-{service.Name}"))
                throw new ArgumentException("Service ID mismatch");
            _statusText.Text = service.GetStatus().Display;
        }
    }
}