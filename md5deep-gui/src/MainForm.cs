using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Text;
using System.Threading;
using System.Windows.Forms;

namespace MD5DeepGui
{
    public class MainForm : Form
    {
        private const string AppTitle = "MD5Deep GUI";
        private const string CreditLine = "md5deep by Jesse Kornblum";
        private const string ProjectUrl = "https://github.com/jessek/hashdeep";

        private const int ColFile = 0;
        private const int ColSize = 1;
        private const int ColMd5 = 2;
        private const int ColSha1 = 3;
        private const int ColSha256 = 4;
        private const int ColSha512 = 5;
        private const int ColStatus = 6;

        private class RowData
        {
            public string Path;
            public long Size;
            public FileHashResult Result;
        }

        private MenuStrip _menu;
        private ListView _list;
        private CheckBox _chkRecursive;
        private CheckBox _chkMd5;
        private CheckBox _chkSha1;
        private CheckBox _chkSha256;
        private CheckBox _chkSha512;
        private Button _btnAddFiles;
        private Button _btnAddFolder;
        private Button _btnClear;
        private Button _btnCompute;
        private Button _btnCancel;
        private StatusStrip _status;
        private ToolStripProgressBar _progress;
        private ToolStripStatusLabel _statusLabel;
        private ToolStripStatusLabel _creditLabel;
        private ToolStripMenuItem _miSave;
        private ToolStripMenuItem _miClearKnown;

        private readonly HashSet<string> _paths = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        private HashSet<string> _knownHashes;   // null = matching mode off
        private string _knownHashesFile;
        private List<HashJobAlgorithm> _lastRunAlgorithms;

        private Thread _worker;
        private volatile bool _cancel;
        private bool _running;
        private long _totalBytes;
        private long _doneBytes;

        public MainForm()
        {
            Text = AppTitle + " — a tribute to Jesse Kornblum's md5deep";
            Width = 1150;
            Height = 680;
            MinimumSize = new Size(820, 460);
            StartPosition = FormStartPosition.CenterScreen;
            AllowDrop = true;

            BuildMenu();
            BuildList();
            BuildTopPanels();
            BuildStatusBar();

            DragEnter += OnDragEnterForm;
            DragDrop += OnDragDropForm;
            FormClosing += OnFormClosingForm;

            UpdateStatusIdle();
        }

        // ------------------------------------------------------------------ UI

        private void BuildMenu()
        {
            _menu = new MenuStrip();

            var file = new ToolStripMenuItem("&File");
            var miAddFiles = new ToolStripMenuItem("Add &Files…", null, delegate { AddFilesDialog(); });
            var miAddFolder = new ToolStripMenuItem("Add Fol&der…", null, delegate { AddFolderDialog(); });
            _miSave = new ToolStripMenuItem("&Save Results…", null, delegate { SaveResults(); });
            _miSave.Enabled = false;
            var miExit = new ToolStripMenuItem("E&xit", null, delegate { Close(); });
            file.DropDownItems.Add(miAddFiles);
            file.DropDownItems.Add(miAddFolder);
            file.DropDownItems.Add(new ToolStripSeparator());
            file.DropDownItems.Add(_miSave);
            file.DropDownItems.Add(new ToolStripSeparator());
            file.DropDownItems.Add(miExit);

            var match = new ToolStripMenuItem("&Matching");
            var miLoadKnown = new ToolStripMenuItem("&Load Known Hashes (like md5deep -m)…", null, delegate { LoadKnownHashesDialog(); });
            _miClearKnown = new ToolStripMenuItem("&Clear Known Hashes", null, delegate { ClearKnownHashes(); });
            _miClearKnown.Enabled = false;
            match.DropDownItems.Add(miLoadKnown);
            match.DropDownItems.Add(_miClearKnown);

            var help = new ToolStripMenuItem("&Help");
            var miProject = new ToolStripMenuItem("md5deep Project Page (Jesse Kornblum)…", null, delegate { OpenUrl(ProjectUrl); });
            var miAbout = new ToolStripMenuItem("&About…", null, delegate { ShowAbout(); });
            help.DropDownItems.Add(miProject);
            help.DropDownItems.Add(new ToolStripSeparator());
            help.DropDownItems.Add(miAbout);

            _menu.Items.Add(file);
            _menu.Items.Add(match);
            _menu.Items.Add(help);
            MainMenuStrip = _menu;
        }

        private void BuildList()
        {
            _list = new ListView();
            _list.Dock = DockStyle.Fill;
            _list.View = View.Details;
            _list.FullRowSelect = true;
            _list.GridLines = true;
            _list.HideSelection = false;
            _list.ShowItemToolTips = true;
            _list.Columns.Add("File", 380);
            _list.Columns.Add("Size", 80, HorizontalAlignment.Right);
            _list.Columns.Add("MD5", 240);
            _list.Columns.Add("SHA-1", 300);
            _list.Columns.Add("SHA-256", 340);
            _list.Columns.Add("SHA-512", 340);
            _list.Columns.Add("Status", 110);

            var ctx = new ContextMenuStrip();
            ctx.Items.Add(new ToolStripMenuItem("Copy Selected Rows", null, delegate { CopySelectedRows(); }));
            ctx.Items.Add(new ToolStripMenuItem("Remove Selected Rows", null, delegate { RemoveSelectedRows(); }));
            _list.ContextMenuStrip = ctx;

            Controls.Add(_list);
        }

        private void BuildTopPanels()
        {
            // Second row: algorithms (added first so the button row docks above it)
            var row2 = new FlowLayoutPanel();
            row2.Dock = DockStyle.Top;
            row2.AutoSize = true;
            row2.Padding = new Padding(6, 2, 6, 4);
            row2.WrapContents = true;

            var lblAlg = new Label();
            lblAlg.Text = "Algorithms:";
            lblAlg.AutoSize = true;
            lblAlg.Margin = new Padding(3, 8, 3, 3);
            row2.Controls.Add(lblAlg);

            _chkMd5 = MakeAlgCheck("MD5 (md5deep)", true);
            _chkSha1 = MakeAlgCheck("SHA-1 (sha1deep)", false);
            _chkSha256 = MakeAlgCheck("SHA-256 (sha256deep)", true);
            _chkSha512 = MakeAlgCheck("SHA-512", false);
            row2.Controls.Add(_chkMd5);
            row2.Controls.Add(_chkSha1);
            row2.Controls.Add(_chkSha256);
            row2.Controls.Add(_chkSha512);
            Controls.Add(row2);

            // First (top) row: actions
            var row1 = new FlowLayoutPanel();
            row1.Dock = DockStyle.Top;
            row1.AutoSize = true;
            row1.Padding = new Padding(6, 4, 6, 0);
            row1.WrapContents = true;

            _btnAddFiles = MakeButton("Add Files…", delegate { AddFilesDialog(); });
            _btnAddFolder = MakeButton("Add Folder…", delegate { AddFolderDialog(); });
            _chkRecursive = new CheckBox();
            _chkRecursive.Text = "Include subfolders (-r)";
            _chkRecursive.Checked = true;
            _chkRecursive.AutoSize = true;
            _chkRecursive.Margin = new Padding(6, 8, 12, 3);
            _btnClear = MakeButton("Clear List", delegate { ClearList(); });
            _btnCompute = MakeButton("Compute Hashes", delegate { StartRun(); });
            _btnCancel = MakeButton("Cancel", delegate { _cancel = true; });
            _btnCancel.Enabled = false;

            row1.Controls.Add(_btnAddFiles);
            row1.Controls.Add(_btnAddFolder);
            row1.Controls.Add(_chkRecursive);
            row1.Controls.Add(_btnClear);
            row1.Controls.Add(_btnCompute);
            row1.Controls.Add(_btnCancel);
            Controls.Add(row1);

            Controls.Add(_menu);
        }

        private void BuildStatusBar()
        {
            _status = new StatusStrip();
            _progress = new ToolStripProgressBar();
            _progress.Minimum = 0;
            _progress.Maximum = 1000;
            _progress.Width = 220;
            _statusLabel = new ToolStripStatusLabel();
            _statusLabel.Spring = true;
            _statusLabel.TextAlign = ContentAlignment.MiddleLeft;
            _creditLabel = new ToolStripStatusLabel(CreditLine);
            _creditLabel.IsLink = true;
            _creditLabel.Click += delegate { OpenUrl(ProjectUrl); };
            _status.Items.Add(_progress);
            _status.Items.Add(_statusLabel);
            _status.Items.Add(_creditLabel);
            Controls.Add(_status);
        }

        private static CheckBox MakeAlgCheck(string text, bool isChecked)
        {
            var c = new CheckBox();
            c.Text = text;
            c.Checked = isChecked;
            c.AutoSize = true;
            c.Margin = new Padding(6, 6, 6, 3);
            return c;
        }

        private static Button MakeButton(string text, EventHandler onClick)
        {
            var b = new Button();
            b.Text = text;
            b.AutoSize = true;
            b.Padding = new Padding(4, 2, 4, 2);
            b.Click += onClick;
            return b;
        }

        // ------------------------------------------------------- adding files

        private void AddFilesDialog()
        {
            using (var dlg = new OpenFileDialog())
            {
                dlg.Title = "Add files to hash";
                dlg.Multiselect = true;
                if (dlg.ShowDialog(this) == DialogResult.OK)
                    AddPaths(dlg.FileNames);
            }
        }

        private void AddFolderDialog()
        {
            using (var dlg = new FolderBrowserDialog())
            {
                dlg.Description = "Add a folder — every file inside will be hashed" +
                                  (_chkRecursive.Checked ? " (including subfolders)" : "");
                if (dlg.ShowDialog(this) == DialogResult.OK)
                    AddPaths(new string[] { dlg.SelectedPath });
            }
        }

        private void AddPaths(IEnumerable<string> paths)
        {
            var files = new List<string>();
            var errors = new List<string>();
            foreach (var p in paths)
            {
                if (Directory.Exists(p))
                    HashEngine.CollectFiles(p, _chkRecursive.Checked, files, errors);
                else if (File.Exists(p))
                    files.Add(p);
                else
                    errors.Add(p + ": not found");
            }

            _list.BeginUpdate();
            int added = 0;
            foreach (var f in files)
            {
                if (!_paths.Add(f))
                    continue;
                long size = 0;
                try { size = new FileInfo(f).Length; }
                catch (Exception) { }

                var row = new RowData();
                row.Path = f;
                row.Size = size;

                var item = new ListViewItem(f);
                item.UseItemStyleForSubItems = false;
                item.SubItems.Add(HashEngine.FormatSize(size));
                item.SubItems.Add("");   // MD5
                item.SubItems.Add("");   // SHA-1
                item.SubItems.Add("");   // SHA-256
                item.SubItems.Add("");   // SHA-512
                item.SubItems.Add("");   // Status
                item.Tag = row;
                _list.Items.Add(item);
                added++;
            }
            _list.EndUpdate();

            UpdateStatusIdle();
            if (errors.Count > 0)
            {
                var shown = errors.Count > 12 ? errors.GetRange(0, 12) : errors;
                var suffix = errors.Count > 12 ? "\n… and " + (errors.Count - 12) + " more" : "";
                MessageBox.Show(this,
                    "Added " + added + " file(s), but some entries could not be read:\n\n" +
                    string.Join("\n", shown.ToArray()) + suffix,
                    AppTitle, MessageBoxButtons.OK, MessageBoxIcon.Warning);
            }
        }

        private void ClearList()
        {
            if (_running) return;
            _list.Items.Clear();
            _paths.Clear();
            _lastRunAlgorithms = null;
            _miSave.Enabled = false;
            _progress.Value = 0;
            UpdateStatusIdle();
        }

        private void RemoveSelectedRows()
        {
            if (_running) return;
            foreach (ListViewItem item in _list.SelectedItems)
            {
                var row = (RowData)item.Tag;
                _paths.Remove(row.Path);
                _list.Items.Remove(item);
            }
            UpdateStatusIdle();
        }

        private void OnDragEnterForm(object sender, DragEventArgs e)
        {
            if (!_running && e.Data.GetDataPresent(DataFormats.FileDrop))
                e.Effect = DragDropEffects.Copy;
        }

        private void OnDragDropForm(object sender, DragEventArgs e)
        {
            if (_running) return;
            var dropped = e.Data.GetData(DataFormats.FileDrop) as string[];
            if (dropped != null)
                AddPaths(dropped);
        }

        // ----------------------------------------------------------- hashing

        private List<HashJobAlgorithm> SelectedAlgorithms()
        {
            var all = HashEngine.AllAlgorithms();
            var picked = new List<HashJobAlgorithm>();
            foreach (var a in all)
            {
                if (a.Name == "MD5" && _chkMd5.Checked) picked.Add(a);
                if (a.Name == "SHA-1" && _chkSha1.Checked) picked.Add(a);
                if (a.Name == "SHA-256" && _chkSha256.Checked) picked.Add(a);
                if (a.Name == "SHA-512" && _chkSha512.Checked) picked.Add(a);
            }
            return picked;
        }

        private static int ColumnForAlgorithm(string name)
        {
            if (name == "MD5") return ColMd5;
            if (name == "SHA-1") return ColSha1;
            if (name == "SHA-256") return ColSha256;
            return ColSha512;
        }

        private void StartRun()
        {
            if (_running) return;
            if (_list.Items.Count == 0)
            {
                MessageBox.Show(this, "Add files or folders first (or drag && drop them onto the window).",
                    AppTitle, MessageBoxButtons.OK, MessageBoxIcon.Information);
                return;
            }
            var algorithms = SelectedAlgorithms();
            if (algorithms.Count == 0)
            {
                MessageBox.Show(this, "Select at least one hash algorithm.",
                    AppTitle, MessageBoxButtons.OK, MessageBoxIcon.Information);
                return;
            }

            var items = new List<ListViewItem>();
            _totalBytes = 0;
            foreach (ListViewItem item in _list.Items)
            {
                var row = (RowData)item.Tag;
                row.Result = null;
                item.SubItems[ColMd5].Text = "";
                item.SubItems[ColSha1].Text = "";
                item.SubItems[ColSha256].Text = "";
                item.SubItems[ColSha512].Text = "";
                item.SubItems[ColStatus].Text = "";
                _totalBytes += row.Size;
                items.Add(item);
            }

            _lastRunAlgorithms = algorithms;
            _cancel = false;
            _running = true;
            _doneBytes = 0;
            _progress.Value = 0;
            SetUiRunning(true);
            _statusLabel.Text = "Hashing " + items.Count + " file(s)…";

            var sw = Stopwatch.StartNew();
            _worker = new Thread(delegate ()
            {
                int lastTick = Environment.TickCount;
                int done = 0;
                int errors = 0;
                int matches = 0;
                foreach (var item in items)
                {
                    if (_cancel) break;
                    var row = (RowData)item.Tag;
                    var captured = item;
                    var result = HashEngine.HashFile(row.Path, algorithms,
                        delegate (int n)
                        {
                            Interlocked.Add(ref _doneBytes, n);
                            int now = Environment.TickCount;
                            if (now - lastTick > 100)
                            {
                                lastTick = now;
                                BeginInvoke((MethodInvoker)delegate { UpdateProgress(); });
                            }
                        },
                        delegate { return _cancel; });

                    if (result.Cancelled) break;
                    row.Result = result;
                    if (result.Error != null) errors++;
                    else done++;
                    if (result.Error == null && IsMatch(result)) matches++;
                    BeginInvoke((MethodInvoker)delegate { ApplyResultToItem(captured, result); });
                }
                sw.Stop();
                int doneCopy = done, errorsCopy = errors, matchesCopy = matches;
                bool cancelled = _cancel;
                BeginInvoke((MethodInvoker)delegate
                {
                    FinishRun(doneCopy, errorsCopy, matchesCopy, cancelled, sw.Elapsed);
                });
            });
            _worker.IsBackground = true;
            _worker.Start();
        }

        private bool IsMatch(FileHashResult result)
        {
            if (_knownHashes == null) return false;
            foreach (var kv in result.Hashes)
            {
                if (_knownHashes.Contains(kv.Value))
                    return true;
            }
            return false;
        }

        private void ApplyResultToItem(ListViewItem item, FileHashResult result)
        {
            var row = (RowData)item.Tag;
            row.Size = result.Size > 0 ? result.Size : row.Size;
            item.SubItems[ColSize].Text = HashEngine.FormatSize(row.Size);
            foreach (var kv in result.Hashes)
                item.SubItems[ColumnForAlgorithm(kv.Key)].Text = kv.Value;

            var status = item.SubItems[ColStatus];
            if (result.Error != null)
            {
                status.Text = "error";
                status.ForeColor = Color.Firebrick;
                item.ToolTipText = result.Error;
            }
            else if (_knownHashes != null)
            {
                if (IsMatch(result))
                {
                    status.Text = "MATCH";
                    status.ForeColor = Color.ForestGreen;
                }
                else
                {
                    status.Text = "no match";
                    status.ForeColor = Color.Firebrick;
                }
            }
            else
            {
                status.Text = "done";
                status.ForeColor = Color.DimGray;
            }
        }

        private void UpdateProgress()
        {
            long total = _totalBytes;
            long done = Interlocked.Read(ref _doneBytes);
            if (total > 0)
            {
                int v = (int)(done * 1000 / total);
                if (v < 0) v = 0;
                if (v > 1000) v = 1000;
                _progress.Value = v;
            }
            _statusLabel.Text = "Hashing… " + HashEngine.FormatSize(done) + " of " + HashEngine.FormatSize(total);
        }

        private void FinishRun(int done, int errors, int matches, bool cancelled, TimeSpan elapsed)
        {
            _running = false;
            SetUiRunning(false);
            _progress.Value = cancelled ? _progress.Value : 1000;
            _miSave.Enabled = true;

            var sb = new StringBuilder();
            if (cancelled) sb.Append("Cancelled — ");
            sb.Append("hashed ").Append(done).Append(" file(s) in ").Append(elapsed.TotalSeconds.ToString("0.0")).Append(" s");
            if (errors > 0) sb.Append(", ").Append(errors).Append(" error(s)");
            if (_knownHashes != null) sb.Append(", ").Append(matches).Append(" match(es) against ").Append(Path.GetFileName(_knownHashesFile));
            _statusLabel.Text = sb.ToString();
        }

        private void SetUiRunning(bool running)
        {
            _btnAddFiles.Enabled = !running;
            _btnAddFolder.Enabled = !running;
            _btnClear.Enabled = !running;
            _btnCompute.Enabled = !running;
            _btnCancel.Enabled = running;
            _menu.Enabled = !running;
            _chkMd5.Enabled = !running;
            _chkSha1.Enabled = !running;
            _chkSha256.Enabled = !running;
            _chkSha512.Enabled = !running;
            _chkRecursive.Enabled = !running;
        }

        private void OnFormClosingForm(object sender, FormClosingEventArgs e)
        {
            _cancel = true;
        }

        // ---------------------------------------------------------- matching

        private void LoadKnownHashesDialog()
        {
            using (var dlg = new OpenFileDialog())
            {
                dlg.Title = "Load known hashes (md5deep/hashdeep output, CSV, or plain hash list)";
                dlg.Filter = "Hash lists (*.txt;*.md5;*.sha256;*.csv)|*.txt;*.md5;*.sha256;*.csv|All files (*.*)|*.*";
                if (dlg.ShowDialog(this) != DialogResult.OK)
                    return;
                try
                {
                    var set = HashEngine.LoadKnownHashes(dlg.FileName);
                    if (set.Count == 0)
                    {
                        MessageBox.Show(this, "No hashes recognized in that file.",
                            AppTitle, MessageBoxButtons.OK, MessageBoxIcon.Warning);
                        return;
                    }
                    _knownHashes = set;
                    _knownHashesFile = dlg.FileName;
                    _miClearKnown.Enabled = true;
                    RefreshMatchStatuses();
                    _statusLabel.Text = "Loaded " + set.Count + " known hash(es) from " +
                        Path.GetFileName(dlg.FileName) + " — files will be marked MATCH / no match.";
                }
                catch (Exception ex)
                {
                    MessageBox.Show(this, "Could not read the file:\n" + ex.Message,
                        AppTitle, MessageBoxButtons.OK, MessageBoxIcon.Error);
                }
            }
        }

        private void ClearKnownHashes()
        {
            _knownHashes = null;
            _knownHashesFile = null;
            _miClearKnown.Enabled = false;
            RefreshMatchStatuses();
            UpdateStatusIdle();
        }

        private void RefreshMatchStatuses()
        {
            foreach (ListViewItem item in _list.Items)
            {
                var row = (RowData)item.Tag;
                if (row.Result != null)
                    ApplyResultToItem(item, row.Result);
            }
        }

        // ------------------------------------------------------------ output

        private void SaveResults()
        {
            if (_lastRunAlgorithms == null)
            {
                MessageBox.Show(this, "Compute hashes first.", AppTitle,
                    MessageBoxButtons.OK, MessageBoxIcon.Information);
                return;
            }
            using (var dlg = new SaveFileDialog())
            {
                dlg.Title = "Save hash results";
                dlg.Filter = "Plain hash list — md5deep style (*.txt)|*.txt|" +
                             "hashdeep audit format (*.txt)|*.txt|" +
                             "CSV spreadsheet (*.csv)|*.csv";
                dlg.FileName = "hashes";
                if (dlg.ShowDialog(this) != DialogResult.OK)
                    return;
                try
                {
                    using (var w = new StreamWriter(dlg.FileName, false, new UTF8Encoding(false)))
                    {
                        if (dlg.FilterIndex == 1) WritePlain(w);
                        else if (dlg.FilterIndex == 2) WriteHashdeep(w);
                        else WriteCsv(w);
                    }
                    _statusLabel.Text = "Saved results to " + dlg.FileName;
                }
                catch (Exception ex)
                {
                    MessageBox.Show(this, "Could not save the file:\n" + ex.Message,
                        AppTitle, MessageBoxButtons.OK, MessageBoxIcon.Error);
                }
            }
        }

        // Classic md5deep output: "<hash>  <path>", one line per file, grouped
        // per algorithm when several were computed.
        private void WritePlain(TextWriter w)
        {
            foreach (var alg in _lastRunAlgorithms)
            {
                foreach (ListViewItem item in _list.Items)
                {
                    var row = (RowData)item.Tag;
                    if (row.Result == null || row.Result.Error != null) continue;
                    string hash;
                    if (row.Result.Hashes.TryGetValue(alg.Name, out hash))
                        w.WriteLine(hash + "  " + row.Path);
                }
            }
        }

        private void WriteHashdeep(TextWriter w)
        {
            var tokens = new List<string>();
            tokens.Add("size");
            foreach (var alg in _lastRunAlgorithms)
                tokens.Add(alg.FileToken);
            tokens.Add("filename");
            w.WriteLine("%%%% HASHDEEP-1.0");
            w.WriteLine("%%%% " + string.Join(",", tokens.ToArray()));
            w.WriteLine("## Generated by MD5Deep GUI (format from Jesse Kornblum's hashdeep)");
            foreach (ListViewItem item in _list.Items)
            {
                var row = (RowData)item.Tag;
                if (row.Result == null || row.Result.Error != null) continue;
                var fields = new List<string>();
                fields.Add(row.Result.Size.ToString());
                foreach (var alg in _lastRunAlgorithms)
                {
                    string hash;
                    row.Result.Hashes.TryGetValue(alg.Name, out hash);
                    fields.Add(hash ?? "");
                }
                fields.Add(row.Path);
                w.WriteLine(string.Join(",", fields.ToArray()));
            }
        }

        private void WriteCsv(TextWriter w)
        {
            var header = new List<string>();
            header.Add("path");
            header.Add("size_bytes");
            foreach (var alg in _lastRunAlgorithms)
                header.Add(alg.FileToken);
            header.Add("status");
            w.WriteLine(string.Join(",", header.ToArray()));
            foreach (ListViewItem item in _list.Items)
            {
                var row = (RowData)item.Tag;
                var fields = new List<string>();
                fields.Add(CsvQuote(row.Path));
                fields.Add(row.Size.ToString());
                foreach (var alg in _lastRunAlgorithms)
                {
                    string hash = null;
                    if (row.Result != null)
                        row.Result.Hashes.TryGetValue(alg.Name, out hash);
                    fields.Add(hash ?? "");
                }
                fields.Add(CsvQuote(item.SubItems[ColStatus].Text));
                w.WriteLine(string.Join(",", fields.ToArray()));
            }
        }

        private static string CsvQuote(string s)
        {
            if (s == null) return "";
            if (s.IndexOfAny(new char[] { ',', '"', '\n', '\r' }) < 0) return s;
            return "\"" + s.Replace("\"", "\"\"") + "\"";
        }

        private void CopySelectedRows()
        {
            if (_list.SelectedItems.Count == 0) return;
            var sb = new StringBuilder();
            foreach (ListViewItem item in _list.SelectedItems)
            {
                var cells = new List<string>();
                foreach (ListViewItem.ListViewSubItem sub in item.SubItems)
                    cells.Add(sub.Text);
                sb.AppendLine(string.Join("\t", cells.ToArray()));
            }
            try { Clipboard.SetText(sb.ToString()); }
            catch (Exception) { }
        }

        // ------------------------------------------------------------- misc

        private void UpdateStatusIdle()
        {
            long total = 0;
            foreach (ListViewItem item in _list.Items)
                total += ((RowData)item.Tag).Size;
            if (_list.Items.Count == 0)
                _statusLabel.Text = "Add files or a folder (or drag && drop), pick algorithms, then Compute Hashes.";
            else
                _statusLabel.Text = _list.Items.Count + " file(s), " + HashEngine.FormatSize(total) + " queued." +
                    (_knownHashes != null ? " Matching against " + _knownHashes.Count + " known hash(es)." : "");
        }

        private void ShowAbout()
        {
            MessageBox.Show(this,
                AppTitle + " 1.0\n\n" +
                "A native Windows 11 graphical front-end for the md5deep workflow:\n" +
                "recursive file hashing (MD5, SHA-1, SHA-256, SHA-512), known-hash\n" +
                "matching (like md5deep -m), and md5deep/hashdeep-compatible output.\n\n" +
                "Credit: md5deep and hashdeep were created by Jesse Kornblum. His\n" +
                "command-line tools defined this workflow and remain the reference\n" +
                "implementation — this GUI is an independent tribute and is not\n" +
                "affiliated with or endorsed by the original project.\n" +
                ProjectUrl + "\n\n" +
                "md5deep is public domain software; this GUI is public domain too.",
                "About " + AppTitle, MessageBoxButtons.OK, MessageBoxIcon.Information);
        }

        private static void OpenUrl(string url)
        {
            try
            {
                var psi = new ProcessStartInfo(url);
                psi.UseShellExecute = true;
                Process.Start(psi);
            }
            catch (Exception) { }
        }
    }
}
