const makeConfig = require('./common');

const sjsNames = {
  public : 'a',
  home   : 'h',
  project: 'p',
  ww     : 'w',
};

module.exports = makeConfig({

  mode: 'prod',

  name: '[hash:32].[ext]',

  sjsName: n => `${sjsNames[n]}.js`,

  // static resources all go in /s/ as is configured in web.xml
  staticDir: 's',

  htmlMinifyOptions: {
    removeComments: true,
    collapseWhitespace: true,
    minifyCSS: true,
  },
});
