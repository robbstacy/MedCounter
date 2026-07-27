using System;
using System.Reflection;
using System.Windows.Forms;

[assembly: AssemblyTitle("MD5Deep GUI")]
[assembly: AssemblyDescription("A Windows graphical front-end for the md5deep workflow. md5deep was created by Jesse Kornblum; this GUI is an independent tribute and is not affiliated with the original project.")]
[assembly: AssemblyProduct("MD5Deep GUI")]
[assembly: AssemblyCopyright("Public domain, like md5deep itself. Original md5deep by Jesse Kornblum.")]
[assembly: AssemblyVersion("1.0.0.0")]
[assembly: AssemblyFileVersion("1.0.0.0")]

namespace MD5DeepGui
{
    internal static class Program
    {
        [STAThread]
        private static void Main()
        {
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new MainForm());
        }
    }
}
