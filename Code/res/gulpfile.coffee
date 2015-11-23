gulp      = require 'gulp'
concat    = require 'gulp-concat'
debug     = require 'gulp-debug'
del       = require 'del'
expect    = require 'gulp-expect-file'
minifycss = require 'gulp-minify-css'
rename    = require 'gulp-rename'
uglify    = require 'gulp-uglify'

cfg_bower       = 'bower/'
cfg_ws_root     = '../webapp-server'
cfg_ws_webapp   = cfg_ws_root + '/src/main/webapp'
cfg_ws_dev      = cfg_ws_webapp + '/assets'
cfg_ws_prod     = cfg_ws_webapp + '/a'

nonRetardedSrc = (a) -> gulp.src(a).pipe expect a

devProdTasks = (name, srcs, mapBoth, mapDevTask, mapProdTask) ->
  mkSrc = (dev) ->
    devprod = (prefix, d = '', p = '.min', suffix = '.js') -> prefix + (if dev then d else p) + suffix
    nonRetardedSrc srcs devprod
  nameD = name + ':dev'
  nameP = name + ':prod'
  gulp.task(nameD, -> mapDevTask mapBoth mkSrc true)
  gulp.task(nameP, -> mapProdTask mapBoth mkSrc false)
  gulp.task name, [nameD, nameP]

# WEBAPP-CLIENT
#
# create JS for unit tests

# WEBSERVER

# Put everything in devprod assets/ for dev, or a/ for prod.
# Minify everything in prod.
# Even vendor stuff goes under devprod assets/ or a/

# Base            - bootstrap, maybe jq
# project <: base - react, etc, maybe katex too
# x <: y - whatever

# Compile bootstrap
# JS versions ⇒ Scala consts for CDN

gulp.task 'ws:clean', ->
  del [cfg_ws_dev, cfg_ws_prod], force: true

gulp.task 'ws:vendor', ->
  nonRetardedSrc [
      cfg_bower + 'katex/dist/**/*'
      '!**/*.md'
      cfg_bower + 'zeroclipboard/ZeroClipboard.swf'
    ]
    .pipe gulp.dest cfg_ws_dev
    .pipe gulp.dest cfg_ws_prod

###
{src:'<%= cfg.bower %>/jquery/dist/jquery.min.map',      dest:'<%= cfg.assets_dev %>/jquery.min.map',    nonull:true},
              '<%= cfg.js.src %>/google-analytics.js',
              '<%= cfg.bower %>/bootstrap/js/alert.js',
              '<%= cfg.bower %>/bootstrap/js/dropdown.js',
              '<%= cfg.bower %>/bootstrap/js/modal.js',
              '<%= cfg.bower %>/bootstrap/js/tab.js',
              '<%= cfg.bower %>/bootstrap/js/transition.js',
              '<%= cfg.bower %>/jquery.ui/ui/jquery.ui.core.js',
              '<%= cfg.bower %>/jquery.ui/ui/jquery.ui.effect.js',
              '<%= cfg.bower %>/jquery.ui/ui/jquery.ui.effect-drop.js',
              '<%= cfg.bower %>/jquery.ui/ui/jquery.ui.effect-fade.js',
              '<%= cfg.bower %>/jquery.ui/ui/jquery.ui.effect-highlight.js',
              '<%= cfg.bower %>/jquery.ui/ui/jquery.ui.effect-slide.js',
              '<%= cfg.bower %>/jquery-autosize/jquery.autosize.min.js',
              '<%= cfg.bower %>/jquery-timeago/jquery.timeago.js',
              '<%= cfg.bower %>/jquery.livequery/dist/jquery.livequery.min.js',
              '<%= cfg.bower %>/jquery-rangyinputs/rangyinputs-jquery.js',
              '<%= cfg.bower %>/mousetrap/mousetrap.min.js',
              '<%= cfg.bower %>/mousetrap/plugins/global-bind/mousetrap-global-bind.min.js',
###

devProdTasks('ws:project', (f) ->
    [
      cfg_bower + 'jquery/dist/jquery.min.js'
      f(cfg_bower + 'react/react', '-with-addons')
      f(cfg_bower + 'react/react-dom')
      f(cfg_bower + 'jquery-textcomplete/dist/jquery.textcomplete')
    ]
  (b) -> b.pipe concat 'project2.js'
  (d) -> d.pipe gulp.dest cfg_ws_dev
  (p) -> p.pipe gulp.dest cfg_ws_prod
)

###
ws_project_src = (dev) ->
  devprod = (prefix, d = '', p = '.min', suffix = '.js') -> prefix + (if dev then d else p) + suffix
  srcs = [
    cfg_bower + 'jquery/dist/jquery.min.js'
    devprod(cfg_bower + 'react/react', '-with-addons')
    devprod(cfg_bower + 'react/react-dom')
    devprod(cfg_bower + 'jquery-textcomplete/dist/jquery.textcomplete'),
  ]
  nonRetardedSrc(srcs)
    # .pipe debug title: 'ws:project:' + (if dev then "dev" else "prod")

gulp.task 'ws:project:dev', ->
  ws_project_src(true)
    .pipe concat 'project2.js'
    .pipe gulp.dest cfg_ws_dev

gulp.task 'ws:project:prod', ->
  ws_project_src(false)
    .pipe concat 'project.js'
#    .pipe uglify()
    .pipe gulp.dest cfg_ws_prod

gulp.task 'ws:project', ['ws:project:dev','ws:project:prod']
###

gulp.task 'ws', ['ws:clean'], ->
  gulp.start ['ws:vendor', 'ws:project']

gulp.task 'default', ['ws']
