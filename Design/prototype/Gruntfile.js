module.exports = function(grunt) {
  'use strict';

  // Load all the Grunt tasks listed in package.json
  require('matchdep').filterDev('grunt-*').forEach( grunt.loadNpmTasks );

  // ===========================================================================
  grunt.initConfig({

    haml: {
      all: {
        files: [{expand: true, cwd: '', src: ['*.haml'], dest: '', ext: '.html', flatten: false }],
      },
    },

    watch: {
      options: {
        spawn: false,
      },
      haml: { files: ['*.haml'], tasks: ['haml'] },
    },

  });

  // ===========================================================================
  grunt.registerTask('default', ['haml']);

};

// vim:sw=2 ts=2 et:
